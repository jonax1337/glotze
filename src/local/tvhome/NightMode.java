package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/** At night the launcher dims itself and plays its sounds softly; the lock screen editor sets when. */
final class NightMode {
    /** The night until the editor sets another one. */
    static final int STARTS_AT_HOUR = 22;
    static final int ENDS_AT_HOUR = 7;
    private static final String PREFERENCES = "ui";
    private static final String KEY_ENABLED = "night_mode";

    private final SharedPreferences preferences;
    private final NightSchedule schedule;

    NightMode(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        schedule = new NightSchedule(context);
    }

    boolean isEnabled() {
        return preferences.getBoolean(KEY_ENABLED, true);
    }

    void toggle() {
        preferences.edit().putBoolean(KEY_ENABLED, !isEnabled()).apply();
    }

    boolean isActive() {
        return isEnabled() && schedule.isNight(System.currentTimeMillis());
    }
}
