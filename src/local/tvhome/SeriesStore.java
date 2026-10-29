package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The followed series, kept as JSON in the preferences "series". Every method locks the same
 * monitor, so the home screen, the sync thread and the playback detector can use separate instances.
 */
final class SeriesStore {
    private static final String TAG = "SeriesStore";
    private static final String PREFERENCES = "series";
    private static final String KEY_LIST = "list";
    private static final String KEY_AUTO_DETECT = "auto_detect";
    private static final Object LOCK = new Object();

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String IMAGE_URL = "image";
    private static final String NETWORK = "network";
    private static final String NEXT = "next";
    private static final String PREVIOUS = "previous";
    private static final String ADDED_AT = "added";
    private static final String LAST_DETECTED_AT = "detected";
    private static final String ACKNOWLEDGED = "acknowledged";
    private static final String LAUNCH_PACKAGE = "app";
    private static final String AUTO = "auto";
    private static final String SYNCED_AT = "synced";
    private static final String SEASON = "season";
    private static final String NUMBER = "number";
    private static final String EPISODE_NAME = "title";
    private static final String AIR_MILLIS = "air";

    private final SharedPreferences preferences;

    SeriesStore(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    /** Copies in the order they were stored; changing one changes nothing until it is saved. */
    List<Series> all() {
        synchronized (LOCK) {
            return read();
        }
    }

    boolean isFollowed(long id) {
        synchronized (LOCK) {
            return indexOf(read(), id) >= 0;
        }
    }

    /** Adds the show if it is new. A show that is already followed only has its detection time renewed when {@code auto}. */
    void follow(ShowHit hit, boolean auto) {
        synchronized (LOCK) {
            List<Series> list = read();
            long now = System.currentTimeMillis();
            int index = indexOf(list, hit.id);
            if (index < 0) {
                list.add(newSeries(hit, auto, now));
            } else if (auto) {
                list.get(index).lastDetectedAt = now;
            } else {
                return;
            }
            write(list);
        }
    }

    void unfollow(long id) {
        synchronized (LOCK) {
            List<Series> list = read();
            int index = indexOf(list, id);
            if (index >= 0) {
                list.remove(index);
                write(list);
            }
        }
    }

    boolean autoDetectEnabled() {
        return preferences.getBoolean(KEY_AUTO_DETECT, true);
    }

    void setAutoDetectEnabled(boolean on) {
        preferences.edit().putBoolean(KEY_AUTO_DETECT, on).apply();
    }

    /** Replaces the stored series with the same id; a series that has been unfollowed meanwhile stays gone. */
    void save(Series series) {
        synchronized (LOCK) {
            List<Series> list = read();
            int index = indexOf(list, series.id);
            if (index >= 0) {
                list.set(index, series);
                write(list);
            }
        }
    }

    /** Changes the stored series in one step, so a concurrent change to another field is not lost. */
    void update(long id, Consumer<Series> change) {
        synchronized (LOCK) {
            List<Series> list = read();
            int index = indexOf(list, id);
            if (index >= 0) {
                change.accept(list.get(index));
                write(list);
            }
        }
    }

    private static Series newSeries(ShowHit hit, boolean auto, long now) {
        Series series = new Series(hit.id, hit.name, now);
        series.imageUrl = hit.imageUrl;
        series.networkName = hit.networkName;
        series.auto = auto;
        series.lastDetectedAt = auto ? now : 0;
        return series;
    }

    private static int indexOf(List<Series> list, long id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    private List<Series> read() {
        List<Series> list = new ArrayList<>();
        try {
            JSONArray stored = new JSONArray(preferences.getString(KEY_LIST, "[]"));
            for (int i = 0; i < stored.length(); i++) {
                list.add(fromJson(stored.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "The stored series are unreadable and are dropped", e);
            list.clear();
        }
        return list;
    }

    private void write(List<Series> list) {
        try {
            JSONArray stored = new JSONArray();
            for (Series series : list) {
                stored.put(toJson(series));
            }
            preferences.edit().putString(KEY_LIST, stored.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "The series could not be stored", e);
        }
    }

    private static Series fromJson(JSONObject json) throws JSONException {
        Series series = new Series(json.getLong(ID), json.getString(NAME), json.optLong(ADDED_AT));
        series.imageUrl = optionalText(json, IMAGE_URL);
        series.networkName = optionalText(json, NETWORK);
        series.next = episodeFromJson(json.optJSONObject(NEXT));
        series.previous = episodeFromJson(json.optJSONObject(PREVIOUS));
        series.lastDetectedAt = json.optLong(LAST_DETECTED_AT);
        series.acknowledgedAirMillis = json.optLong(ACKNOWLEDGED);
        series.launchPackage = optionalText(json, LAUNCH_PACKAGE);
        series.auto = json.optBoolean(AUTO);
        series.syncedAt = json.optLong(SYNCED_AT);
        return series;
    }

    private static JSONObject toJson(Series series) throws JSONException {
        return new JSONObject()
                .put(ID, series.id)
                .put(NAME, series.name)
                .put(IMAGE_URL, series.imageUrl)
                .put(NETWORK, series.networkName)
                .put(NEXT, episodeToJson(series.next))
                .put(PREVIOUS, episodeToJson(series.previous))
                .put(ADDED_AT, series.addedAt)
                .put(LAST_DETECTED_AT, series.lastDetectedAt)
                .put(ACKNOWLEDGED, series.acknowledgedAirMillis)
                .put(LAUNCH_PACKAGE, series.launchPackage)
                .put(AUTO, series.auto)
                .put(SYNCED_AT, series.syncedAt);
    }

    private static EpisodeInfo episodeFromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        return new EpisodeInfo(json.optInt(SEASON), json.optInt(NUMBER),
                optionalText(json, EPISODE_NAME), json.optLong(AIR_MILLIS));
    }

    private static JSONObject episodeToJson(EpisodeInfo episode) throws JSONException {
        if (episode == null) {
            return null;
        }
        return new JSONObject()
                .put(SEASON, episode.season)
                .put(NUMBER, episode.number)
                .put(EPISODE_NAME, episode.name)
                .put(AIR_MILLIS, episode.airMillis);
    }

    /** JSONObject.optString turns a JSON null into the text "null", so nulls are checked first. */
    private static String optionalText(JSONObject json, String key) {
        return json.isNull(key) ? null : json.optString(key);
    }
}
