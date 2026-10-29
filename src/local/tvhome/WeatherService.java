package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Local weather without an account: the place is the home address from PlaceSettings, or else comes
 * from the public IP (ipapi.co, once a day); the forecast from Open-Meteo (every half hour). Both are cached, so the weather shows instantly.
 */
final class WeatherService {
    interface Listener {
        void onWeather(Weather weather);

        void onWeatherUnavailable();
    }

    private static final String TAG = "WeatherService";
    private static final String PREFERENCES = "weather";
    private static final String KEY_PLACE = "place";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LOCATED_AT = "located_at";
    // Versioned: a forecast cached in an older shape would not parse, and would block refreshes until stale.
    private static final String KEY_FORECAST = "forecast_v2";
    private static final String KEY_FORECAST_AT = "forecast_v2_at";
    private static final long LOCATION_MAX_AGE_MS = TimeUnit.DAYS.toMillis(1);
    private static final long FORECAST_MAX_AGE_MS = TimeUnit.MINUTES.toMillis(30);
    /** Today and tomorrow: enough hourly data to look {@link Weather#HOURS_AHEAD} hours ahead late at night. */
    private static final int FORECAST_DAYS = 2;
    private static final String LOCATION_URL = "https://ipapi.co/json/";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=%.4f&longitude=%.4f"
            + "&current=temperature_2m,weather_code,is_day"
            + "&hourly=temperature_2m,weather_code,is_day"
            + "&daily=sunrise,sunset"
            + "&timezone=auto&forecast_days=%d";

    private final SharedPreferences cache;
    private final PlaceSettings places;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    WeatherService(Context context) {
        this.cache = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        this.places = new PlaceSettings(context);
    }

    /** Shows the cached forecast at once, then refreshes it in the background if it has gone stale. */
    void load(Listener listener) {
        cachedWeather().ifPresent(listener::onWeather);
        if (isOlderThan(KEY_FORECAST_AT, FORECAST_MAX_AGE_MS)) {
            worker.execute(() -> refreshIfStillStale(listener));
        }
    }

    void shutdown() {
        worker.shutdownNow();
    }

    private void refreshIfStillStale(Listener listener) {
        if (!isOlderThan(KEY_FORECAST_AT, FORECAST_MAX_AGE_MS)) {
            return;
        }
        try {
            Weather weather = fetchWeather();
            mainThread.post(() -> listener.onWeather(weather));
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Weather refresh failed; keeping what is on screen", e);
            if (!cachedWeather().isPresent()) {
                mainThread.post(listener::onWeatherUnavailable);
            }
        }
    }

    /** The next {@link #load} asks for a fresh forecast, e.g. after the home address changed. */
    void forgetForecast() {
        cache.edit().remove(KEY_FORECAST_AT).apply();
    }

    private Weather fetchWeather() throws IOException, JSONException {
        PlaceSettings.Home home = places.home();
        if (home != null) {
            storeHome(home);
        } else if (isOlderThan(KEY_LOCATED_AT, LOCATION_MAX_AGE_MS)) {
            storeLocation(HttpJson.get(LOCATION_URL));
        }
        JSONObject forecast = HttpJson.get(forecastUrl());
        Weather weather = parse(forecast);
        cache.edit()
                .putString(KEY_FORECAST, forecast.toString())
                .putLong(KEY_FORECAST_AT, System.currentTimeMillis())
                .apply();
        return weather;
    }

    private void storeLocation(JSONObject location) throws JSONException {
        store(location.getString("city"), location.getDouble("latitude"), location.getDouble("longitude"));
    }

    /** The home address reads "Street 1, District": the district is the place to show. */
    private void storeHome(PlaceSettings.Home home) {
        String address = home.address;
        store(address.substring(address.lastIndexOf(',') + 1).trim(), home.latitude, home.longitude);
    }

    private void store(String place, double latitude, double longitude) {
        cache.edit()
                .putString(KEY_PLACE, place)
                .putFloat(KEY_LATITUDE, (float) latitude)
                .putFloat(KEY_LONGITUDE, (float) longitude)
                .putLong(KEY_LOCATED_AT, System.currentTimeMillis())
                .apply();
    }

    private String forecastUrl() {
        return String.format(Locale.US, FORECAST_URL,
                cache.getFloat(KEY_LATITUDE, 0f), cache.getFloat(KEY_LONGITUDE, 0f), FORECAST_DAYS);
    }

    private Optional<Weather> cachedWeather() {
        String forecast = cache.getString(KEY_FORECAST, "");
        if (forecast.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(new JSONObject(forecast)));
        } catch (JSONException e) {
            Log.w(TAG, "Ignoring unreadable cached forecast", e);
            return Optional.empty();
        }
    }

    private Weather parse(JSONObject forecast) throws JSONException {
        return Weather.fromOpenMeteo(cache.getString(KEY_PLACE, ""), forecast);
    }

    private boolean isOlderThan(String timestampKey, long maxAgeMs) {
        return System.currentTimeMillis() - cache.getLong(timestampKey, 0L) > maxAgeMs;
    }
}
