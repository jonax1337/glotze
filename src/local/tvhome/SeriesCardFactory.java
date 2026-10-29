package local.tvhome;

import android.content.res.Resources;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Turns a followed series into what the home screen shows: its title, the episode that matters and whether it is new. */
final class SeriesCardFactory {
    private static final long FRESH_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(14);
    private static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final int FIRST_EPISODE = 1;
    /** Calendar.DAY_OF_WEEK counts from Sunday = 1; the weekday names start with Monday. */
    private static final int MONDAY_FIRST_SHIFT = 5;
    private static final int DAYS_PER_WEEK = 7;
    private static final int RECENT_DAYS = 14;
    private static final int YEAR_CENTURY = 100;

    private final Resources resources;

    SeriesCardFactory(Resources resources) {
        this.resources = resources;
    }

    /** True for an episode of the last 14 days that the user has not opened yet. */
    static boolean isFresh(Series series, long now) {
        EpisodeInfo latest = series.previous;
        return latest != null
                && latest.airMillis > series.acknowledgedAirMillis
                && latest.airMillis <= now
                && now - latest.airMillis <= FRESH_WINDOW_MILLIS;
    }

    SeriesCard cardOf(Series series, long now) {
        boolean fresh = isFresh(series, now);
        return new SeriesCard(series.id, series.name, subtitleOf(series, fresh, now), fresh, series.imageUrl);
    }

    /**
     * A new episode first; otherwise the coming one ("Neue Staffel" if it opens a season);
     * otherwise the last one, which reads "zuletzt" with its date once it is older than two weeks.
     */
    private String subtitleOf(Series series, boolean fresh, long now) {
        if (fresh) {
            return describeWithDay(series.previous, now);
        }
        if (series.next != null) {
            return series.next.number == FIRST_EPISODE
                    ? resources.getString(R.string.series_new_season)
                    : describeWithDay(series.next, now);
        }
        return series.previous == null ? "" : describeLast(series.previous, now);
    }

    private String describeWithDay(EpisodeInfo episode, long now) {
        return episode.airMillis <= 0 ? undated(episode) : dated(episode, dayText(episode.airMillis, now));
    }

    private String describeLast(EpisodeInfo episode, long now) {
        return episode.airMillis <= 0 ? undated(episode) : dated(episode, lastDayText(episode.airMillis, now));
    }

    private String undated(EpisodeInfo episode) {
        return resources.getString(R.string.series_episode_undated, episode.season, episode.number);
    }

    private String dated(EpisodeInfo episode, String dayText) {
        return resources.getString(R.string.series_episode_dated, episode.season, episode.number, dayText);
    }

    /** Today, yesterday, tomorrow or the weekday with the date. */
    private String dayText(long airMillis, long now) {
        long daysFromNow = localDay(airMillis) - localDay(now);
        if (daysFromNow == 0) {
            return resources.getString(R.string.series_day_today);
        }
        if (daysFromNow == -1) {
            return resources.getString(R.string.series_day_yesterday);
        }
        if (daysFromNow == 1) {
            return resources.getString(R.string.series_day_tomorrow);
        }
        return weekdayAndDate(airMillis, now);
    }

    /** For an episode that is over: how many days ago while recent, else the date it aired. */
    private String lastDayText(long airMillis, long now) {
        long daysAgo = localDay(now) - localDay(airMillis);
        if (daysAgo < 0) {
            return dayText(airMillis, now);
        }
        if (daysAgo > RECENT_DAYS) {
            return resources.getString(R.string.series_day_last_date, dayOfMonth(airMillis), month(airMillis), twoDigitYear(airMillis));
        }
        if (daysAgo <= 1) {
            return dayText(airMillis, now);
        }
        return resources.getString(R.string.series_day_days_ago, daysAgo);
    }

    /** The year is added only when it is not the current one. */
    private String weekdayAndDate(long airMillis, long now) {
        Calendar day = calendarOf(airMillis);
        String[] weekdays = resources.getStringArray(R.array.series_weekdays_short);
        String weekday = weekdays[(day.get(Calendar.DAY_OF_WEEK) + MONDAY_FIRST_SHIFT) % DAYS_PER_WEEK];
        if (day.get(Calendar.YEAR) == calendarOf(now).get(Calendar.YEAR)) {
            return resources.getString(R.string.series_day_weekday_date, weekday, dayOfMonth(airMillis), month(airMillis));
        }
        return resources.getString(R.string.series_day_weekday_date_year,
                weekday, dayOfMonth(airMillis), month(airMillis), twoDigitYear(airMillis));
    }

    private static Calendar calendarOf(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    private static int dayOfMonth(long millis) {
        return calendarOf(millis).get(Calendar.DAY_OF_MONTH);
    }

    private static int month(long millis) {
        return calendarOf(millis).get(Calendar.MONTH) + 1;
    }

    private static int twoDigitYear(long millis) {
        return calendarOf(millis).get(Calendar.YEAR) % YEAR_CENTURY;
    }

    private static long localDay(long millis) {
        return Math.floorDiv(millis + TimeZone.getDefault().getOffset(millis), DAY_MILLIS);
    }
}
