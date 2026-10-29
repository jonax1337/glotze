package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * The places the lock screen widgets show, kept on the device: the stops of the departures widget
 * and the city of the world clock. Until something is chosen, the defaults from lock_style.xml apply.
 */
final class PlaceSettings {
    static final int MAX_STOPS = 4;

    /** A public transport stop, by the id the BVG API knows it under, and how long the walk to it takes. */
    static final class Stop {
        final String id;
        final String name;
        final int walkMinutes;

        Stop(String id, String name) {
            this(id, name, 0);
        }

        Stop(String id, String name, int walkMinutes) {
            this.id = id;
            this.name = name;
            this.walkMinutes = walkMinutes;
        }
    }

    /** Where the user lives: the stops around it are the ones worth showing. */
    static final class Home {
        final String address;
        final double latitude;
        final double longitude;

        Home(String address, double latitude, double longitude) {
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /** A city and the time zone its clock follows. */
    static final class City {
        final String name;
        final String zoneId;

        City(String name, String zoneId) {
            this.name = name;
            this.zoneId = zoneId;
        }
    }

    private static final String PREFERENCES = "places";
    private static final String KEY_STOP_COUNT = "stop_count";
    private static final String KEY_STOP_ID = "stop_id_";
    private static final String KEY_STOP_NAME = "stop_name_";
    private static final String KEY_STOP_WALK = "stop_walk_";
    private static final String KEY_HOME_ADDRESS = "home_address";
    private static final String KEY_HOME_LATITUDE = "home_latitude";
    private static final String KEY_HOME_LONGITUDE = "home_longitude";
    private static final String KEY_CITY_NAME = "city_name";
    private static final String KEY_CITY_ZONE = "city_zone";

    private final SharedPreferences preferences;
    private final Resources res;

    PlaceSettings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        res = context.getResources();
    }

    /** In the order they were chosen; empty when the user removed them all. */
    List<Stop> stops() {
        if (!preferences.contains(KEY_STOP_COUNT)) {
            return defaultStops();
        }
        int count = preferences.getInt(KEY_STOP_COUNT, 0);
        List<Stop> stops = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stops.add(new Stop(preferences.getString(KEY_STOP_ID + i, ""),
                    preferences.getString(KEY_STOP_NAME + i, ""), preferences.getInt(KEY_STOP_WALK + i, 0)));
        }
        return stops;
    }

    /** Null until an address was chosen. */
    Home home() {
        if (!preferences.contains(KEY_HOME_ADDRESS)) {
            return null;
        }
        return new Home(preferences.getString(KEY_HOME_ADDRESS, ""),
                Double.longBitsToDouble(preferences.getLong(KEY_HOME_LATITUDE, 0)),
                Double.longBitsToDouble(preferences.getLong(KEY_HOME_LONGITUDE, 0)));
    }

    void saveHome(Home home) {
        preferences.edit().putString(KEY_HOME_ADDRESS, home.address)
                .putLong(KEY_HOME_LATITUDE, Double.doubleToRawLongBits(home.latitude))
                .putLong(KEY_HOME_LONGITUDE, Double.doubleToRawLongBits(home.longitude)).apply();
    }

    void saveStops(List<Stop> stops) {
        if (stops.size() > MAX_STOPS) {
            throw new IllegalArgumentException("At most " + MAX_STOPS + " stops can be saved, got " + stops.size());
        }
        SharedPreferences.Editor editor = preferences.edit().putInt(KEY_STOP_COUNT, stops.size());
        for (int i = 0; i < stops.size(); i++) {
            editor.putString(KEY_STOP_ID + i, stops.get(i).id).putString(KEY_STOP_NAME + i, stops.get(i).name)
                    .putInt(KEY_STOP_WALK + i, stops.get(i).walkMinutes);
        }
        editor.apply();
    }

    City worldClock() {
        if (!preferences.contains(KEY_CITY_ZONE)) {
            return new City(res.getString(R.string.world_clock_city), res.getString(R.string.world_clock_zone));
        }
        return new City(preferences.getString(KEY_CITY_NAME, ""), preferences.getString(KEY_CITY_ZONE, ""));
    }

    void saveWorldClock(City city) {
        preferences.edit().putString(KEY_CITY_NAME, city.name).putString(KEY_CITY_ZONE, city.zoneId).apply();
    }

    /** The cities offered for the world clock, alphabetical. */
    List<City> cityChoices() {
        String[] names = res.getStringArray(R.array.places_city_names);
        String[] zoneIds = res.getStringArray(R.array.places_city_zones);
        List<City> cities = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            cities.add(new City(names[i], zoneIds[i]));
        }
        return cities;
    }

    /** A stop without a name of its own is shown by its id. */
    private List<Stop> defaultStops() {
        String[] ids = res.getStringArray(R.array.departure_stops);
        String[] names = res.getStringArray(R.array.places_default_stop_names);
        List<Stop> stops = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            stops.add(new Stop(ids[i], i < names.length ? names[i] : ids[i]));
        }
        return stops;
    }
}
