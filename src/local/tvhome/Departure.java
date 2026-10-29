package local.tvhome;

import android.content.res.Resources;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** One train or bus leaving a stop, as the BVG API reports it. */
final class Departure {
    private static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String[] NAME_NOISE = {" (Berlin)", " Bhf"};

    final long leavesAt;
    final String line;
    final String product;
    final String direction;

    Departure(JSONObject json) throws JSONException, ParseException {
        leavesAt = new SimpleDateFormat(TIME_FORMAT, Locale.ROOT).parse(json.getString("when")).getTime();
        JSONObject lineJson = json.getJSONObject("line");
        line = lineJson.getString("name");
        product = lineJson.optString("product");
        direction = withoutNoise(json.optString("direction"));
    }

    /** "jetzt" in the last minute, otherwise the minutes left. */
    String minutesUntil(Resources res) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(leavesAt - System.currentTimeMillis());
        return minutes < 1 ? res.getString(R.string.departure_now) : res.getString(R.string.departure_minutes, minutes);
    }

    private static String withoutNoise(String stopName) {
        String name = stopName;
        for (String noise : NAME_NOISE) {
            name = name.replace(noise, "");
        }
        return name;
    }
}
