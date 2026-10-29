package local.tvhome;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The next departures from the stops chosen in PlaceSettings, soonest first, side by side in one
 * pill, from the free BVG API. Refreshed every minute while the screen saver shows.
 */
final class DeparturesWidget implements LockWidget {
    private static final long REFRESH_MS = TimeUnit.MINUTES.toMillis(1);
    private static final int SHOWN = 3;

    private final LinearLayout pill;
    private final LayoutInflater inflater;
    private final Fonts fonts;
    private final List<PlaceSettings.Stop> stops;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::refresh;

    DeparturesWidget(LayoutInflater inflater, ViewGroup parent, Fonts fonts) {
        this.inflater = inflater;
        this.fonts = fonts;
        pill = (LinearLayout) inflater.inflate(R.layout.lock_widget_departures, parent, false);
        Resources res = pill.getResources();
        pill.setBackground(SurfaceDrawable.card(res, res.getDimension(R.dimen.lock_weather_height) / 2f));
        stops = new PlaceSettings(pill.getContext()).stops();
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
        worker.execute(() -> {
            List<Departure> upcoming = DepartureFeed.upcoming(stops);
            List<Departure> next = upcoming.subList(0, Math.min(SHOWN, upcoming.size()));
            mainThread.post(() -> show(next));
        });
        mainThread.postDelayed(refresh, REFRESH_MS);
    }

    /** With nothing to show, the earlier departures stay rather than an empty pill. */
    private void show(List<Departure> departures) {
        if (departures.isEmpty()) {
            return;
        }
        pill.removeAllViews();
        for (Departure departure : departures) {
            pill.addView(segmentFor(departure));
        }
        fonts.applyTo(pill);
        pill.setVisibility(View.VISIBLE);
    }

    private View segmentFor(Departure departure) {
        Resources res = pill.getResources();
        View segment = inflater.inflate(R.layout.lock_departure_row, pill, false);
        TextView line = segment.findViewById(R.id.departure_line);
        line.setText(departure.line);
        line.setBackground(badge(res.getColor(R.color.glotze_lime, null), res.getDimension(R.dimen.row_corner) / 2f));
        ((TextView) segment.findViewById(R.id.departure_direction)).setText(departure.direction);
        ((TextView) segment.findViewById(R.id.departure_minutes)).setText(departure.minutesUntil(res));
        return segment;
    }

    private static GradientDrawable badge(int colour, float corner) {
        GradientDrawable badge = new GradientDrawable();
        badge.setColor(colour);
        badge.setCornerRadius(corner);
        return badge;
    }
}
