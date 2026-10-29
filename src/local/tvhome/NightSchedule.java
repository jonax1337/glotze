package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * When the night is: between the hours chosen in the lock screen editor, or from sunset to sunrise
 * at the place WeatherService located. Without a place, or where the sun does not rise or set, the
 * chosen hours apply.
 */
final class NightSchedule {
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;
    private static final double HOURS_PER_DAY = 24;
    private static final double DEGREES_PER_HOUR = 15;
    private static final double FULL_CIRCLE = 360;
    private static final double QUARTER_CIRCLE = 90;

    // Written by WeatherService when it finds the place.
    private static final String WEATHER_PREFERENCES = "weather";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";

    // Almanac for Computers (1990), as published by Ed Williams: sunrise and sunset to within minutes.
    /** The sun's centre is 90.833 degrees from the zenith when its upper edge meets the horizon. */
    private static final double HORIZON_ZENITH = 90.833;
    private static final double RISING_HOUR = 6;
    private static final double SETTING_HOUR = 18;

    /** From the first minute of the night until the first minute of the day, over midnight if need be. */
    private static final class Window {
        private final int fromMinute;
        private final int untilMinute;

        Window(int fromMinute, int untilMinute) {
            this.fromMinute = fromMinute;
            this.untilMinute = untilMinute;
        }

        boolean contains(int minuteOfDay) {
            if (fromMinute > untilMinute) {
                return minuteOfDay >= fromMinute || minuteOfDay < untilMinute;
            }
            return minuteOfDay >= fromMinute && minuteOfDay < untilMinute;
        }
    }

    private final LockStyle style;
    private final SharedPreferences weather;

    NightSchedule(Context context) {
        style = new LockStyle(context);
        weather = context.getSharedPreferences(WEATHER_PREFERENCES, Context.MODE_PRIVATE);
    }

    boolean isNight(long nowMillis) {
        Window night = style.nightFollowsSun()
                ? sunWindow(nowMillis).orElseGet(this::chosenHours)
                : chosenHours();
        return night.contains(minuteOfDay(nowMillis));
    }

    private Window chosenHours() {
        return new Window(style.nightStartHour() * MINUTES_PER_HOUR, style.nightEndHour() * MINUTES_PER_HOUR);
    }

    private Optional<Window> sunWindow(long nowMillis) {
        if (!weather.contains(KEY_LATITUDE) || !weather.contains(KEY_LONGITUDE)) {
            return Optional.empty();
        }
        double latitude = weather.getFloat(KEY_LATITUDE, 0f);
        double longitude = weather.getFloat(KEY_LONGITUDE, 0f);
        int dayOfYear = dayOfYear(nowMillis);
        double sunriseUtc = sunEventUtcHours(dayOfYear, latitude, longitude, true);
        double sunsetUtc = sunEventUtcHours(dayOfYear, latitude, longitude, false);
        if (Double.isNaN(sunriseUtc) || Double.isNaN(sunsetUtc)) {
            return Optional.empty();
        }
        double utcOffsetHours = TimeZone.getDefault().getOffset(nowMillis) / (double) TimeUnit.HOURS.toMillis(1);
        return Optional.of(new Window(
                localMinuteOfDay(sunsetUtc + utcOffsetHours), localMinuteOfDay(sunriseUtc + utcOffsetHours)));
    }

    /** UTC hour of sunrise or sunset, or NaN on a day when the sun does not rise or set there. */
    private static double sunEventUtcHours(int dayOfYear, double latitude, double longitude, boolean rising) {
        double longitudeHours = longitude / DEGREES_PER_HOUR;
        double approximateDay = dayOfYear + ((rising ? RISING_HOUR : SETTING_HOUR) - longitudeHours) / HOURS_PER_DAY;
        double meanAnomaly = 0.9856 * approximateDay - 3.289;
        double sunLongitude = sunLongitude(meanAnomaly);
        double sinDeclination = 0.39782 * Math.sin(Math.toRadians(sunLongitude));
        double cosDeclination = Math.cos(Math.asin(sinDeclination));
        double cosHourAngle = (Math.cos(Math.toRadians(HORIZON_ZENITH))
                - sinDeclination * Math.sin(Math.toRadians(latitude)))
                / (cosDeclination * Math.cos(Math.toRadians(latitude)));
        if (Math.abs(cosHourAngle) > 1) {
            return Double.NaN;
        }
        double hourAngle = Math.toDegrees(Math.acos(cosHourAngle));
        double hoursBeforeNoon = (FULL_CIRCLE - hourAngle) / DEGREES_PER_HOUR;
        double hoursAfterNoon = hourAngle / DEGREES_PER_HOUR;
        double localMeanTime = (rising ? hoursBeforeNoon : hoursAfterNoon)
                + rightAscensionHours(sunLongitude) - 0.06571 * approximateDay - 6.622;
        return wrap(localMeanTime - longitudeHours, HOURS_PER_DAY);
    }

    private static double sunLongitude(double meanAnomaly) {
        double anomaly = Math.toRadians(meanAnomaly);
        return wrap(meanAnomaly + 1.916 * Math.sin(anomaly) + 0.020 * Math.sin(2 * anomaly) + 282.634, FULL_CIRCLE);
    }

    /** Right ascension lies in the same quarter of the circle as the sun's longitude. */
    private static double rightAscensionHours(double sunLongitude) {
        double ascension = wrap(Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(sunLongitude)))), FULL_CIRCLE);
        double sameQuarter = Math.floor(sunLongitude / QUARTER_CIRCLE) * QUARTER_CIRCLE
                - Math.floor(ascension / QUARTER_CIRCLE) * QUARTER_CIRCLE;
        return (ascension + sameQuarter) / DEGREES_PER_HOUR;
    }

    private static double wrap(double value, double range) {
        return ((value % range) + range) % range;
    }

    private static int localMinuteOfDay(double localHours) {
        return (int) Math.round(wrap(localHours, HOURS_PER_DAY) * MINUTES_PER_HOUR) % MINUTES_PER_DAY;
    }

    private static int dayOfYear(long millis) {
        return calendarAt(millis).get(Calendar.DAY_OF_YEAR);
    }

    private static int minuteOfDay(long millis) {
        Calendar time = calendarAt(millis);
        return time.get(Calendar.HOUR_OF_DAY) * MINUTES_PER_HOUR + time.get(Calendar.MINUTE);
    }

    private static Calendar calendarAt(long millis) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(millis);
        return time;
    }
}
