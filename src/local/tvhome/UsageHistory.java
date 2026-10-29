package local.tvhome;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The launcher's own long-term memory of how much each app is used. Android sees every app
 * opened, from the home screen, by voice or with a remote's app button, but forgets the
 * details after a week or two; so each catch-up folds only the events since the last one
 * into a score kept here for good. Each session earns a point and its length a capped bonus,
 * so a player left running all night does not outrank the app opened every evening. Scores
 * lose half their weight every two weeks, so the order follows habits as they change.
 * Reading the statistics needs a grant over ADB: {@code appops set local.tvhome GET_USAGE_STATS allow};
 * without it, launches from this home screen are counted instead.
 */
final class UsageHistory {
    // Versioned: the first version counted home-screen launches that the statistics also contain.
    private static final String PREFERENCES = "usage_v2";
    private static final String SCORE_SUFFIX = ".score";
    private static final String SCORED_AT_SUFFIX = ".at";
    private static final String LAST_USED_SUFFIX = ".last";
    private static final String KEY_CAUGHT_UP_TO = "caught_up_to";
    private static final String KEY_APP_IN_FRONT = "app_in_front";
    private static final String KEY_IN_FRONT_SINCE = "in_front_since";
    private static final double HALF_LIFE_MS = TimeUnit.DAYS.toMillis(14);
    /** The first catch-up takes in whatever history Android still has. */
    private static final long FIRST_LOOKBACK_MS = TimeUnit.DAYS.toMillis(28);
    /** A session this long earns one extra point; longer earns nothing more. */
    private static final double FULL_SESSION_MINUTES = 90;

    private final Context context;
    private final SharedPreferences preferences;
    private final UsageStatsManager systemStats;

    UsageHistory(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        this.systemStats = context.getSystemService(UsageStatsManager.class);
    }

    /** Counts a launch from this home screen, unless Android's statistics already see it. */
    void recordLaunch(String key) {
        if (maySeeSystemStats()) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Double> gained = new HashMap<>();
        gained.put(key, 1.0);
        Map<String, Long> lastUsed = new HashMap<>();
        lastUsed.put(key, now);
        store(gained, lastUsed, now);
    }

    /** Folds the sessions Android has seen since the last catch-up into the stored scores. */
    void catchUp() {
        if (!maySeeSystemStats()) {
            return;
        }
        long now = System.currentTimeMillis();
        String inFront = preferences.getString(KEY_APP_IN_FRONT, "");
        long inFrontSince = preferences.getLong(KEY_IN_FRONT_SINCE, now);
        Map<String, Double> gained = new HashMap<>();
        Map<String, Long> lastUsed = new HashMap<>();
        UsageEvents events = systemStats.queryEvents(
                preferences.getLong(KEY_CAUGHT_UP_TO, now - FIRST_LOOKBACK_MS), now);
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String app = event.getPackageName();
            // ACTIVITY_RESUMED has the value of MOVE_TO_FOREGROUND, which Android 7 reports.
            if (event.getEventType() != UsageEvents.Event.ACTIVITY_RESUMED || app.equals(inFront)) {
                continue;
            }
            long time = event.getTimeStamp();
            gain(gained, inFront, lengthBonus(time - inFrontSince), time, now);
            gain(gained, app, 1, time, now);
            if (countsAsUsage(app)) {
                lastUsed.put(app, time);
            }
            inFront = app;
            inFrontSince = time;
        }
        store(gained, lastUsed, now);
        preferences.edit()
                .putString(KEY_APP_IN_FRONT, inFront)
                .putLong(KEY_IN_FRONT_SINCE, inFrontSince)
                .putLong(KEY_CAUGHT_UP_TO, now)
                .apply();
    }

    Comparator<AppTile> mostUsedFirst() {
        long now = System.currentTimeMillis();
        return Comparator.comparingDouble((AppTile tile) -> -scoreAt(tile.key, now));
    }

    /** When the app last came to the front, or 0 if it never did while this history was watching. */
    long lastUsedAt(String key) {
        return preferences.getLong(key + LAST_USED_SUFFIX, 0L);
    }

    Comparator<AppTile> mostRecentFirst() {
        return Comparator.comparingLong((AppTile tile) -> -lastUsedAt(tile.key));
    }

    /** Points earned at {@code time}, already worn down to their weight at {@code now}. */
    private void gain(Map<String, Double> gained, String app, double points, long time, long now) {
        if (countsAsUsage(app)) {
            gained.merge(app, points * decay(now - time), Double::sum);
        }
    }

    private boolean countsAsUsage(String app) {
        return !app.isEmpty() && !app.equals(context.getPackageName());
    }

    private void store(Map<String, Double> gained, Map<String, Long> lastUsed, long now) {
        SharedPreferences.Editor edit = preferences.edit();
        for (Map.Entry<String, Double> app : gained.entrySet()) {
            edit.putFloat(app.getKey() + SCORE_SUFFIX, (float) (scoreAt(app.getKey(), now) + app.getValue()))
                    .putLong(app.getKey() + SCORED_AT_SUFFIX, now);
        }
        for (Map.Entry<String, Long> app : lastUsed.entrySet()) {
            edit.putLong(app.getKey() + LAST_USED_SUFFIX, app.getValue());
        }
        edit.apply();
    }

    private double scoreAt(String key, long now) {
        float score = preferences.getFloat(key + SCORE_SUFFIX, 0f);
        long scoredAt = preferences.getLong(key + SCORED_AT_SUFFIX, now);
        return score * decay(now - scoredAt);
    }

    private boolean maySeeSystemStats() {
        AppOpsManager appOps = context.getSystemService(AppOpsManager.class);
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName())
                == AppOpsManager.MODE_ALLOWED;
    }

    private static double lengthBonus(long sessionMs) {
        return Math.min(TimeUnit.MILLISECONDS.toMinutes(sessionMs), FULL_SESSION_MINUTES) / FULL_SESSION_MINUTES;
    }

    private static double decay(long ageMs) {
        return Math.pow(0.5, Math.max(0, ageMs) / HALF_LIFE_MS);
    }
}
