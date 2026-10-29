package local.tvhome;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A small chip with the next departure that can still be caught from the stops chosen in PlaceSettings:
 * the line in a lime badge, then the direction and the minutes left. Asked again every minute while home is
 * showing; with nothing to show, or offline, the chip stays hidden.
 */
final class DepartureChip {
    private static final long REFRESH_MS = TimeUnit.MINUTES.toMillis(1);

    private final View chip;
    private final TextView line;
    private final TextView summary;
    private final PlaceSettings places;
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::refresh;
    /** Null while stopped; results of an earlier worker are dropped. */
    private ExecutorService worker;

    DepartureChip(View chip) {
        this.chip = chip;
        this.line = chip.findViewById(R.id.departure_chip_line);
        this.summary = chip.findViewById(R.id.departure_chip_text);
        this.places = new PlaceSettings(chip.getContext());
        Resources res = chip.getResources();
        chip.setBackground(SurfaceDrawable.card(res, res.getDimension(R.dimen.stage_chip_corner)));
        line.setBackground(limeBadge(res));
    }

    void start() {
        worker = Executors.newSingleThreadExecutor();
        refresh();
    }

    void stop() {
        mainThread.removeCallbacks(refresh);
        if (worker != null) {
            worker.shutdownNow();
            worker = null;
        }
    }

    private void refresh() {
        ExecutorService current = worker;
        List<PlaceSettings.Stop> stops = places.stops();
        current.execute(() -> {
            List<Departure> upcoming = DepartureFeed.upcoming(stops);
            mainThread.post(() -> {
                if (current == worker) {
                    show(upcoming);
                }
            });
        });
        mainThread.postDelayed(refresh, REFRESH_MS);
    }

    private void show(List<Departure> upcoming) {
        if (upcoming.isEmpty()) {
            chip.setVisibility(View.GONE);
            return;
        }
        Departure next = upcoming.get(0);
        Resources res = chip.getResources();
        line.setText(next.line);
        summary.setText(res.getString(R.string.stage_departure_chip, next.direction, next.minutesUntil(res)));
        chip.setVisibility(View.VISIBLE);
    }

    private static GradientDrawable limeBadge(Resources res) {
        GradientDrawable badge = new GradientDrawable();
        badge.setColor(res.getColor(R.color.glotze_lime, null));
        badge.setCornerRadius(res.getDimension(R.dimen.stage_badge_corner));
        return badge;
    }
}
