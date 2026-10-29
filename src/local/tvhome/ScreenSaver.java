package local.tvhome;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;

import java.util.concurrent.TimeUnit;

/**
 * Which screen saver Fire OS starts when the TV is idle (ours or Amazon's), and after how long.
 * Android only lets an app change this with permissions granted once over ADB:
 * {@code pm grant local.tvhome android.permission.WRITE_SECURE_SETTINGS} and
 * {@code appops set local.tvhome WRITE_SETTINGS allow}.
 */
final class ScreenSaver {
    // Settings.Secure names that Android keeps hidden from apps.
    private static final String KEY_COMPONENTS = "screensaver_components";
    private static final String KEY_ENABLED = "screensaver_enabled";
    private static final String KEY_SLEEP = "sleep_timeout";
    private static final String FIRE_TV_SCREEN_SAVER =
            "com.amazon.ftv.screensaver/.app.services.ScreensaverService";
    private static final String FIRE_TV_AMBIENT = "com.amazon.tv.ftvambient";
    private static final String PREFERENCES = "screensaver";
    private static final String KEY_PREVIOUS = "previous";
    /** How much earlier than the system the launcher shows its own screen saver, see {@link #showsOnlyFromHome}. */
    private static final long LEAD_MS = TimeUnit.SECONDS.toMillis(30);
    private static final long MIN_IDLE_MS = TimeUnit.SECONDS.toMillis(30);

    private final Context context;
    private final ContentResolver settings;
    private final ComponentName own;
    private final SharedPreferences preferences;

    ScreenSaver(Context context) {
        this.context = context;
        this.settings = context.getContentResolver();
        this.own = new ComponentName(context, LockScreenDream.class);
        this.preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    boolean isOwn() {
        String current = Settings.Secure.getString(settings, KEY_COMPONENTS);
        return current != null && own.equals(ComponentName.unflattenFromString(current));
    }

    boolean canChoose() {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Switches between ours and the one that was active before, Amazon's if none was recorded. */
    void toggle() {
        if (isOwn()) {
            Settings.Secure.putString(settings, KEY_COMPONENTS,
                    preferences.getString(KEY_PREVIOUS, FIRE_TV_SCREEN_SAVER));
            return;
        }
        preferences.edit()
                .putString(KEY_PREVIOUS, Settings.Secure.getString(settings, KEY_COMPONENTS))
                .apply();
        Settings.Secure.putString(settings, KEY_COMPONENTS, own.flattenToString());
        Settings.Secure.putInt(settings, KEY_ENABLED, 1);
    }

    /**
     * Fire OS 8 never starts the chosen screen saver: its Ambient Experience takes over, and with
     * that switched off the screen simply goes dark. There the home screen shows ours itself,
     * shortly before the system would step in.
     */
    boolean showsOnlyFromHome() {
        try {
            context.getPackageManager().getApplicationInfo(FIRE_TV_AMBIENT, PackageManager.MATCH_DISABLED_COMPONENTS);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    long homeIdleMs() {
        return Math.max(MIN_IDLE_MS, systemIdleMs() - LEAD_MS);
    }

    /** Fire OS lets the TV sleep this long after the last key press; zero or less means never. */
    boolean sleepsWhenIdle() {
        return systemSleepMs() > 0;
    }

    /** How long the screen saver shown from home stays lit before the TV may sleep. */
    long awakeMsOnceShown() {
        return Math.max(MIN_IDLE_MS, systemSleepMs() - homeIdleMs());
    }

    int sleepMinutes() {
        return (int) TimeUnit.MILLISECONDS.toMinutes(systemSleepMs());
    }

    void setSleepMinutes(int minutes) {
        Settings.Secure.putInt(settings, KEY_SLEEP, (int) TimeUnit.MINUTES.toMillis(minutes));
    }

    private long systemSleepMs() {
        return Settings.Secure.getInt(settings, KEY_SLEEP, 0);
    }

    /** The screen saver starts when the screen would otherwise go to sleep. */
    int idleMinutes() {
        return (int) TimeUnit.MILLISECONDS.toMinutes(systemIdleMs());
    }

    private long systemIdleMs() {
        return Settings.System.getInt(settings, Settings.System.SCREEN_OFF_TIMEOUT, 0);
    }

    boolean canChangeIdleTime() {
        return Settings.System.canWrite(context);
    }

    void setIdleMinutes(int minutes) {
        Settings.System.putInt(settings, Settings.System.SCREEN_OFF_TIMEOUT,
                (int) TimeUnit.MINUTES.toMillis(minutes));
    }
}
