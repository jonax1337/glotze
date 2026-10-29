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
import java.util.List;

/** Asks the free BVG API what leaves the given stops next. Blocks on the network: call it off the main thread. */
final class DepartureFeed {
    private static final String TAG = "DepartureFeed";
    private static final String DEPARTURES_URL =
            "https://v6.bvg.transport.rest/stops/%s/departures?duration=60&results=8&remarks=false";

    private DepartureFeed() {
    }

    /** Everything leaving the stops from now on, soonest first; a stop that cannot be reached is left out. */
    static List<Departure> upcoming(List<PlaceSettings.Stop> stops) {
        long now = System.currentTimeMillis();
        List<Departure> upcoming = new ArrayList<>();
        for (Departure departure : fetchAll(stops)) {
            if (departure.leavesAt >= now) {
                upcoming.add(departure);
            }
        }
        Collections.sort(upcoming, Comparator.comparingLong(departure -> departure.leavesAt));
        return upcoming;
    }

    private static List<Departure> fetchAll(List<PlaceSettings.Stop> stops) {
        List<Departure> all = new ArrayList<>();
        for (PlaceSettings.Stop stop : stops) {
            try {
                JSONArray departures = HttpJson.get(String.format(DEPARTURES_URL, stop.id)).getJSONArray("departures");
                addLeaving(departures, all);
            } catch (IOException | JSONException e) {
                Log.w(TAG, "Departures for stop " + stop.id + " unavailable", e);
            }
        }
        return all;
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
