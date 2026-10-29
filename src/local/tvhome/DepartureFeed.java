package local.tvhome;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Asks the free BVG API what leaves the given stops next. Blocks on the network: call it off the main thread. */
final class DepartureFeed {
    private static final String TAG = "DepartureFeed";
    private static final String DEPARTURES_URL =
            "https://v6.bvg.transport.rest/stops/%s/departures?duration=%d&results=40&remarks=false";
    /** How far past the walk the departures reach, so that busy stops still show more than the next few minutes. */
    private static final int WINDOW_AFTER_WALK_MINUTES = 30;

    private DepartureFeed() {
    }

    /**
     * What can still be caught, soonest first: only departures after the walk to their stop, and each
     * line towards each direction once, so that a busy line does not crowd out the others. A stop that
     * cannot be reached is left out.
     */
    static List<Departure> upcoming(List<PlaceSettings.Stop> stops) {
        List<Departure> reachable = new ArrayList<>();
        for (PlaceSettings.Stop stop : stops) {
            reachable.addAll(reachableFrom(stop));
        }
        Collections.sort(reachable, Comparator.comparingLong(departure -> departure.leavesAt));
        return firstOfEachLineAndDirection(reachable);
    }

    private static List<Departure> reachableFrom(PlaceSettings.Stop stop) {
        long catchableFrom = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(stop.walkMinutes);
        List<Departure> reachable = new ArrayList<>();
        for (Departure departure : fetch(stop)) {
            if (departure.leavesAt >= catchableFrom) {
                reachable.add(departure);
            }
        }
        return reachable;
    }

    private static List<Departure> fetch(PlaceSettings.Stop stop) {
        List<Departure> all = new ArrayList<>();
        String url = String.format(Locale.ROOT, DEPARTURES_URL, stop.id, stop.walkMinutes + WINDOW_AFTER_WALK_MINUTES);
        try {
            addLeaving(HttpJson.get(url).getJSONArray("departures"), all);
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Departures for stop " + stop.id + " unavailable", e);
        }
        return all;
    }

    private static List<Departure> firstOfEachLineAndDirection(List<Departure> soonestFirst) {
        Set<String> seen = new HashSet<>();
        List<Departure> distinct = new ArrayList<>();
        for (Departure departure : soonestFirst) {
            if (seen.add(departure.line + ">" + departure.direction)) {
                distinct.add(departure);
            }
        }
        return distinct;
    }

    /** Cancelled trips have no departure time and are skipped. */
    private static void addLeaving(JSONArray departures, List<Departure> into) throws JSONException {
        for (int i = 0; i < departures.length(); i++) {
            JSONObject departure = departures.getJSONObject(i);
            if (departure.isNull("when")) {
                continue;
            }
            try {
                into.add(new Departure(departure));
            } catch (ParseException e) {
                Log.w(TAG, "Unreadable departure time " + departure.optString("when"), e);
            }
        }
    }
}
