package local.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Current conditions, the next hours and the sun times, read from an
 * Open-Meteo forecast. Times are local "yyyy-MM-ddTHH:mm" strings, so they compare as text.
 */
final class Weather {
    static final class Hour {
        /** Hour of day, 0 to 23. */
        final int hour;
        final WeatherCondition condition;
        final boolean daytime;
        final int temperature;

        private Hour(int hour, WeatherCondition condition, boolean daytime, int temperature) {
            this.hour = hour;
            this.condition = condition;
            this.daytime = daytime;
            this.temperature = temperature;
        }
    }

    /** How many hours the forecast looks ahead. */
    static final int HOURS_AHEAD = 12;
    private static final int TODAY = 0;
    private static final int TOMORROW = 1;
    private static final int HOUR_PREFIX = "yyyy-MM-ddTHH".length();
    private static final int CLOCK_START = "yyyy-MM-ddT".length();
    private static final int CLOCK_END = "yyyy-MM-ddTHH:mm".length();

    final String place;
    final int temperature;
    final WeatherCondition condition;
    final boolean daytime;
    /** "HH:mm" of the observation, to tell whether the sun has risen or set yet. */
    final String observedAt;
    final String sunrise;
    final String sunset;
    final String sunriseTomorrow;
    /** Starts with the current hour. */
    final List<Hour> hours;

    private Weather(String place, JSONObject current, JSONObject daily, List<Hour> hours) throws JSONException {
        this.place = place;
        this.temperature = rounded(current.getDouble("temperature_2m"));
        this.condition = WeatherCondition.fromWmoCode(current.getInt("weather_code"));
        this.daytime = current.getInt("is_day") == 1;
        this.observedAt = clockOf(current.getString("time"));
        this.sunrise = clockOf(daily.getJSONArray("sunrise").getString(TODAY));
        this.sunset = clockOf(daily.getJSONArray("sunset").getString(TODAY));
        this.sunriseTomorrow = clockOf(daily.getJSONArray("sunrise").getString(TOMORROW));
        this.hours = Collections.unmodifiableList(hours);
    }

    static Weather fromOpenMeteo(String place, JSONObject forecast) throws JSONException {
        JSONObject current = forecast.getJSONObject("current");
        String currentHour = current.getString("time").substring(0, HOUR_PREFIX);
        return new Weather(place, current, forecast.getJSONObject("daily"),
                hoursFrom(forecast.getJSONObject("hourly"), currentHour));
    }

    private static List<Hour> hoursFrom(JSONObject hourly, String firstHour) throws JSONException {
        JSONArray times = hourly.getJSONArray("time");
        List<Hour> hours = new ArrayList<>();
        for (int i = 0; i < times.length() && hours.size() < HOURS_AHEAD; i++) {
            String time = times.getString(i);
            if (time.substring(0, HOUR_PREFIX).compareTo(firstHour) >= 0) {
                hours.add(hourAt(hourly, i, time));
            }
        }
        return hours;
    }

    private static Hour hourAt(JSONObject hourly, int i, String time) throws JSONException {
        return new Hour(
                Integer.parseInt(time.substring(CLOCK_START, CLOCK_START + 2)),
                WeatherCondition.fromWmoCode(hourly.getJSONArray("weather_code").getInt(i)),
                hourly.getJSONArray("is_day").getInt(i) == 1,
                rounded(hourly.getJSONArray("temperature_2m").getDouble(i)));
    }

    private static String clockOf(String isoTime) {
        return isoTime.substring(CLOCK_START, CLOCK_END);
    }

    private static int rounded(double value) {
        return (int) Math.round(value);
    }
}
