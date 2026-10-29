package local.tvhome;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The next appointments of the visible calendars, side by side in one pill. It looks a week ahead,
 * is refreshed every quarter of an hour while the screen saver shows, and says so kindly when
 * there is nothing to show or the calendar is not shared with the launcher.
 */
final class CalendarWidget implements LockWidget {
    static final String PERMISSION = Manifest.permission.READ_CALENDAR;

    private static final String TAG = "CalendarWidget";
    private static final long REFRESH_MS = TimeUnit.MINUTES.toMillis(15);
    private static final long LOOKAHEAD_MS = TimeUnit.DAYS.toMillis(7);
    private static final int SHOWN = 3;
    private static final String[] PROJECTION = {
            CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY};
    private static final int COLUMN_TITLE = 0;
    private static final int COLUMN_BEGIN = 1;
    private static final int COLUMN_ALL_DAY = 2;
    private static final String VISIBLE_CALENDARS = CalendarContract.Instances.VISIBLE + " = 1";
    private static final String SOONEST_FIRST = CalendarContract.Instances.BEGIN + " ASC";
    private static final String TIME_FORMAT = "HH:mm";
    private static final String WEEKDAY_TIME_FORMAT = "EEE HH:mm";
    private static final String WEEKDAY_FORMAT = "EEE";
    /** All-day appointments are stored as midnight to midnight in UTC, whatever the time zone. */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final int DAY_KEY_FACTOR = 1000;

    /** One appointment, or the start of one that is still going on. */
    private static final class Appointment {
        final String title;
        final long beginMillis;
        final boolean allDay;

        Appointment(String title, long beginMillis, boolean allDay) {
            this.title = title;
            this.beginMillis = beginMillis;
            this.allDay = allDay;
        }
    }

    private final LinearLayout pill;
    private final LayoutInflater inflater;
    private final Fonts fonts;
    private final Context context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::refresh;

    CalendarWidget(LayoutInflater inflater, ViewGroup parent, Fonts fonts) {
        this.inflater = inflater;
        this.fonts = fonts;
        pill = (LinearLayout) inflater.inflate(R.layout.lock_widget_calendar, parent, false);
        context = pill.getContext();
        Resources res = pill.getResources();
        pill.setBackground(SurfaceDrawable.card(res, res.getDimension(R.dimen.lock_weather_height) / 2f));
    }

    static boolean hasAccess(Context context) {
        return context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public View view() {
        return pill;
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void stop() {
        mainThread.removeCallbacks(refresh);
        worker.shutdownNow();
    }

    private void refresh() {
        mainThread.postDelayed(refresh, REFRESH_MS);
        if (!hasAccess(context)) {
            showMessage(R.string.schoner_calendar_no_access);
            return;
        }
        worker.execute(() -> {
            List<Appointment> next = upcoming();
            mainThread.post(() -> show(next));
        });
    }

    /** Runs off the main thread. Without a readable calendar there is simply nothing to show. */
    private List<Appointment> upcoming() {
        long now = System.currentTimeMillis();
        List<Appointment> upcoming = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                instancesBetween(now, now + LOOKAHEAD_MS), PROJECTION, VISIBLE_CALENDARS, null, SOONEST_FIRST)) {
            while (cursor != null && cursor.moveToNext() && upcoming.size() < SHOWN) {
                upcoming.add(new Appointment(cursor.getString(COLUMN_TITLE), cursor.getLong(COLUMN_BEGIN),
                        cursor.getInt(COLUMN_ALL_DAY) == 1));
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Calendar access was withdrawn", e);
        }
        return upcoming;
    }

    private static Uri instancesBetween(long fromMillis, long untilMillis) {
        Uri.Builder range = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(range, fromMillis);
        ContentUris.appendId(range, untilMillis);
        return range.build();
    }

    private void show(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            showMessage(R.string.schoner_calendar_empty);
            return;
        }
        pill.removeAllViews();
        for (Appointment appointment : appointments) {
            pill.addView(rowFor(appointment));
        }
        reveal();
    }

    private void showMessage(int messageRes) {
        pill.removeAllViews();
        View row = inflater.inflate(R.layout.lock_calendar_row, pill, false);
        row.findViewById(R.id.calendar_when).setVisibility(View.GONE);
        ((TextView) row.findViewById(R.id.calendar_title)).setText(messageRes);
        pill.addView(row);
        reveal();
    }

    private void reveal() {
        fonts.applyTo(pill);
        pill.setVisibility(View.VISIBLE);
    }

    private View rowFor(Appointment appointment) {
        View row = inflater.inflate(R.layout.lock_calendar_row, pill, false);
        ((TextView) row.findViewById(R.id.calendar_when)).setText(whenOf(appointment));
        String title = appointment.title == null || appointment.title.isEmpty()
                ? pill.getResources().getString(R.string.schoner_calendar_untitled) : appointment.title;
        ((TextView) row.findViewById(R.id.calendar_title)).setText(title);
        return row;
    }

    /** The time today, the weekday and time later; all-day appointments have no time. */
    private String whenOf(Appointment appointment) {
        TimeZone zone = appointment.allDay ? UTC : TimeZone.getDefault();
        boolean startsByToday = dayKey(appointment.beginMillis, zone)
                <= dayKey(System.currentTimeMillis(), TimeZone.getDefault());
        if (appointment.allDay) {
            return startsByToday ? pill.getResources().getString(R.string.schoner_calendar_today)
                    : format(WEEKDAY_FORMAT, appointment.beginMillis, zone);
        }
        return format(startsByToday ? TIME_FORMAT : WEEKDAY_TIME_FORMAT, appointment.beginMillis, zone);
    }

    private static String format(String pattern, long millis, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.GERMAN);
        format.setTimeZone(zone);
        return format.format(new Date(millis));
    }

    /** Year and day of year as one number, so days compare in order. */
    private static int dayKey(long millis, TimeZone zone) {
        Calendar day = Calendar.getInstance(zone);
        day.setTimeInMillis(millis);
        return day.get(Calendar.YEAR) * DAY_KEY_FACTOR + day.get(Calendar.DAY_OF_YEAR);
    }
}
