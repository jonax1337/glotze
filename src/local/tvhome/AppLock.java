package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Remembers which apps need a PIN and checks that PIN. Only a salted PBKDF2 hash of the PIN is
 * stored, and repeated wrong guesses lock the input for longer and longer.
 *
 * The protection is honest but narrow: it only covers launches made through this launcher. The
 * app itself stays reachable through the system settings, another launcher or adb.
 */
final class AppLock {
    /** What a PIN check came to. */
    enum State { OK, WRONG, LOCKED_UNTIL }

    static final class Verdict {
        private static final Verdict OK = new Verdict(State.OK, 0);
        private static final Verdict WRONG = new Verdict(State.WRONG, 0);

        final State state;
        /** Wall-clock time when guessing is allowed again; only meaningful for LOCKED_UNTIL. */
        final long lockedUntilMillis;

        private Verdict(State state, long lockedUntilMillis) {
            this.state = state;
            this.lockedUntilMillis = lockedUntilMillis;
        }

        private static Verdict lockedUntil(long lockedUntilMillis) {
            return new Verdict(State.LOCKED_UNTIL, lockedUntilMillis);
        }
    }

    static final int MIN_PIN_LENGTH = 4;
    static final int MAX_PIN_LENGTH = 6;

    private static final String PREFERENCES = "app_lock";
    private static final String KEY_SALT = "salt";
    private static final String KEY_HASH = "hash";
    private static final String KEY_LOCKED = "locked";
    private static final String KEY_FAILURES = "failures";
    private static final String KEY_LOCKED_UNTIL = "locked_until";

    private static final String KEY_DERIVATION = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 20000;
    private static final int HASH_BITS = 160;
    private static final int SALT_BYTES = 16;

    private static final int FREE_ATTEMPTS = 5;
    private static final long FIRST_LOCKOUT_MS = 30_000L;
    private static final int MAX_DOUBLINGS = 7;
    private static final long MAX_LOCKOUT_MS = FIRST_LOCKOUT_MS << MAX_DOUBLINGS;

    private final SharedPreferences preferences;
    private final SecureRandom random = new SecureRandom();

    AppLock(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    boolean hasPin() {
        return preferences.contains(KEY_HASH);
    }

    /** Without a PIN nothing counts as locked, since no one could ever unlock it again. */
    boolean isLocked(String packageName) {
        return hasPin() && lockedPackages().contains(packageName);
    }

    void setLocked(String packageName, boolean locked) {
        Set<String> packages = lockedPackages();
        if (locked) {
            packages.add(packageName);
        } else {
            packages.remove(packageName);
        }
        preferences.edit().putStringSet(KEY_LOCKED, packages).apply();
    }

    /**
     * Checks a PIN guess. While a lockout runs the guess is not even looked at, so waiting is the
     * only way out. Without a stored PIN every guess is simply wrong.
     */
    Verdict verify(String pin) {
        long now = System.currentTimeMillis();
        long lockedUntil = lockoutEnd(now);
        if (lockedUntil > now) {
            return Verdict.lockedUntil(lockedUntil);
        }
        if (!hasPin()) {
            return Verdict.WRONG;
        }
        if (matchesStoredPin(pin)) {
            forgetFailures();
            return Verdict.OK;
        }
        return recordFailure(now);
    }

    /** Replaces the PIN. Whoever calls this is responsible for having verified the old one. */
    void setPin(String pin) {
        if (!isWellFormed(pin)) {
            throw new IllegalArgumentException(
                    "A PIN has " + MIN_PIN_LENGTH + " to " + MAX_PIN_LENGTH + " digits");
        }
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        preferences.edit()
                .putString(KEY_SALT, encode(salt))
                .putString(KEY_HASH, encode(derive(pin, salt)))
                .remove(KEY_FAILURES)
                .remove(KEY_LOCKED_UNTIL)
                .commit();
    }

    /** Removes the PIN together with every lock that depended on it. */
    void clearPin() {
        preferences.edit().clear().commit();
    }

    private Verdict recordFailure(long now) {
        int failures = preferences.getInt(KEY_FAILURES, 0) + 1;
        // commit(): a force-stop right after a wrong guess must not hand the guess back.
        SharedPreferences.Editor edit = preferences.edit().putInt(KEY_FAILURES, failures);
        if (failures < FREE_ATTEMPTS) {
            edit.commit();
            return Verdict.WRONG;
        }
        long lockedUntil = now + lockoutLength(failures - FREE_ATTEMPTS);
        edit.putLong(KEY_LOCKED_UNTIL, lockedUntil).commit();
        return Verdict.lockedUntil(lockedUntil);
    }

    private static long lockoutLength(int failuresBeyondFree) {
        return FIRST_LOCKOUT_MS << Math.min(failuresBeyondFree, MAX_DOUBLINGS);
    }

    /**
     * A clock set back must not stretch the lockout beyond the longest one we ever hand out. The
     * cap is written back, otherwise it would slide along with the clock and never run out.
     */
    private long lockoutEnd(long now) {
        long stored = preferences.getLong(KEY_LOCKED_UNTIL, 0);
        long latestAllowed = now + MAX_LOCKOUT_MS;
        if (stored <= latestAllowed) {
            return stored;
        }
        preferences.edit().putLong(KEY_LOCKED_UNTIL, latestAllowed).commit();
        return latestAllowed;
    }

    private void forgetFailures() {
        preferences.edit().remove(KEY_FAILURES).remove(KEY_LOCKED_UNTIL).apply();
    }

    private boolean matchesStoredPin(String pin) {
        if (!isWellFormed(pin)) {
            return false;
        }
        byte[] salt = decode(preferences.getString(KEY_SALT, ""));
        byte[] expected = decode(preferences.getString(KEY_HASH, ""));
        return MessageDigest.isEqual(expected, derive(pin, salt));
    }

    private static boolean isWellFormed(String pin) {
        return pin.matches("[0-9]{" + MIN_PIN_LENGTH + "," + MAX_PIN_LENGTH + "}");
    }

    private static byte[] derive(String pin, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(KEY_DERIVATION).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PIN key derivation is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static byte[] decode(String text) {
        return Base64.decode(text, Base64.NO_WRAP);
    }

    private Set<String> lockedPackages() {
        // getStringSet's result must not be modified, so always hand out a copy.
        return new HashSet<>(preferences.getStringSet(KEY_LOCKED, Collections.emptySet()));
    }
}
