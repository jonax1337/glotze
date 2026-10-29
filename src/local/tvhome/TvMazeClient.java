package local.tvhome;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The free TVmaze API (no key). Every call blocks: use them off the main thread. Failures surface
 * as IOException, an unreadable answer included.
 */
final class TvMazeClient {
    private static final String TAG = "TvMazeClient";
    private static final String BASE_URL = "https://api.tvmaze.com";
    private static final String SEARCH_URL = BASE_URL + "/search/shows?q=";
    private static final String SINGLE_SEARCH_URL = BASE_URL + "/singlesearch/shows?q=";
    private static final String SHOW_URL = BASE_URL + "/shows/%d?embed%%5B%%5D=nextepisode&embed%%5B%%5D=previousepisode";
    private static final String AIRSTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String AIRDATE_FORMAT = "yyyy-MM-dd";
    private static final int YEAR_DIGITS = 4;

    private TvMazeClient() {
    }

    static List<ShowHit> search(String query) throws IOException {
        try {
            JSONArray results = HttpJson.getArray(SEARCH_URL + encode(query));
            List<ShowHit> hits = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                hits.add(parseHit(results.getJSONObject(i).getJSONObject("show")));
            }
            return hits;
        } catch (JSONException e) {
            throw unreadable("search for \"" + query + "\"", e);
        }
    }

    /** The best match, or null if TVmaze knows no such show. */
    static ShowHit singleSearch(String query) throws IOException {
        try {
            return parseHit(HttpJson.get(SINGLE_SEARCH_URL + encode(query)));
        } catch (FileNotFoundException notFound) {
            return null;
        } catch (JSONException e) {
            throw unreadable("single search for \"" + query + "\"", e);
        }
    }

    static ShowDetails details(long id) throws IOException {
        try {
            JSONObject show = HttpJson.get(String.format(Locale.ROOT, SHOW_URL, id));
            JSONObject embedded = show.optJSONObject("_embedded");
            return new ShowDetails(parseHit(show),
                    embedded == null ? null : parseEpisode(embedded.optJSONObject("nextepisode")),
                    embedded == null ? null : parseEpisode(embedded.optJSONObject("previousepisode")));
        } catch (JSONException e) {
            throw unreadable("details of show " + id, e);
        }
    }

    private static String encode(String query) throws IOException {
        return URLEncoder.encode(query, StandardCharsets.UTF_8.name());
    }

    private static IOException unreadable(String request, JSONException cause) {
        return new IOException("TVmaze answered " + request + " with something unreadable", cause);
    }

    private static ShowHit parseHit(JSONObject show) throws JSONException {
        JSONObject image = show.optJSONObject("image");
        JSONObject network = show.optJSONObject("network");
        if (network == null) {
            network = show.optJSONObject("webChannel");
        }
        return new ShowHit(show.getLong("id"), show.getString("name"),
                yearOf(optionalText(show, "premiered")),
                image == null ? null : secure(optionalText(image, "medium")),
                network == null ? null : optionalText(network, "name"),
                optionalText(show, "type"));
    }

    private static EpisodeInfo parseEpisode(JSONObject episode) {
        if (episode == null) {
            return null;
        }
        return new EpisodeInfo(episode.optInt("season"), episode.optInt("number"),
                optionalText(episode, "name"), airMillisOf(episode));
    }

    /** JSONObject.optString turns a JSON null into the text "null", so nulls are checked first. */
    private static String optionalText(JSONObject object, String key) {
        if (object.isNull(key)) {
            return null;
        }
        String text = object.optString(key).trim();
        return text.isEmpty() ? null : text;
    }

    private static int yearOf(String date) {
        if (date == null || date.length() < YEAR_DIGITS) {
            return 0;
        }
        try {
            return Integer.parseInt(date.substring(0, YEAR_DIGITS));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Premiere date \"" + date + "\" has no year", e);
            return 0;
        }
    }

    private static String secure(String url) {
        return url != null && url.startsWith("http://") ? "https://" + url.substring("http://".length()) : url;
    }

    /** The exact broadcast time from airstamp, else (also when it is unreadable) the day from airdate, else 0. */
    private static long airMillisOf(JSONObject episode) {
        String airstamp = optionalText(episode, "airstamp");
        String airdate = optionalText(episode, "airdate");
        long exact = airstamp == null ? 0 : parseTime(airstamp, AIRSTAMP_FORMAT);
        if (exact > 0 || airdate == null) {
            return exact;
        }
        return parseTime(airdate, AIRDATE_FORMAT);
    }

    private static long parseTime(String text, String pattern) {
        try {
            return new SimpleDateFormat(pattern, Locale.ROOT).parse(text).getTime();
        } catch (ParseException e) {
            Log.w(TAG, "Air time \"" + text + "\" is not in format " + pattern, e);
            return 0;
        }
    }
}
