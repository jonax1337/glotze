package local.tvhome;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/** The widgets chosen in the lock screen style, stacked below the clock. */
final class LockWidgets implements WeatherService.Listener {
    private final LinearLayout row;
    private final LayoutInflater inflater;
    private final Fonts fonts;
    private final WeatherService weather;
    private final List<LockWidget> shown = new ArrayList<>();
    private boolean night;

    LockWidgets(LinearLayout row, Fonts fonts, WeatherService weather) {
        this.row = row;
        this.inflater = LayoutInflater.from(row.getContext());
        this.fonts = fonts;
        this.weather = weather;
    }

    /** Replaces the widgets on screen with fresh ones of the given kinds and starts them. */
    void show(List<LockStyle.Widget> kinds, boolean night) {
        stop();
        shown.clear();
        row.removeAllViews();
        this.night = night;
        for (LockStyle.Widget kind : kinds) {
            LockWidget widget = create(kind);
            shown.add(widget);
            leaveGapBefore(widget.view(), row.getChildCount());
            row.addView(widget.view());
        }
        fonts.applyTo(row);
        weather.load(this);
        for (LockWidget widget : shown) {
            widget.onNight(night);
            widget.start();
        }
    }

    /** The widgets on screen follow when the night begins or ends. */
    void setNight(boolean night) {
        if (night == this.night) {
            return;
        }
        this.night = night;
        for (LockWidget widget : shown) {
            widget.onNight(night);
        }
    }

    void stop() {
        for (LockWidget widget : shown) {
            widget.stop();
        }
    }

    @Override
    public void onWeather(Weather now) {
        for (LockWidget widget : shown) {
            widget.onWeather(now);
        }
    }

    @Override
    public void onWeatherUnavailable() {
        for (LockWidget widget : shown) {
            widget.onWeatherUnavailable();
        }
    }

    /** Keeps the size each widget's layout asks for; only the gap to the one above is added. */
    private void leaveGapBefore(View widget, int index) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) widget.getLayoutParams();
        params.topMargin = index == 0 ? 0 : row.getResources().getDimensionPixelSize(R.dimen.lock_widget_gap);
    }

    private LockWidget create(LockStyle.Widget kind) {
        switch (kind) {
            case HOURS:
                return new HoursWidget(inflater, row, fonts);
            case MOON_AND_WORLD_CLOCK:
                return new MoonWidget(inflater, row);
            case NOW_PLAYING:
                return new NowPlayingWidget(inflater, row);
            case DEPARTURES:
                return new DeparturesWidget(inflater, row, fonts);
            case CALENDAR:
                return new CalendarWidget(inflater, row, fonts);
            case GLOTZE:
                return new GlotzeWidget(inflater, row);
            default:
                return new WeatherPillWidget(inflater, row);
        }
    }
}
