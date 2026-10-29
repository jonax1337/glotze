package local.tvhome;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The next hours and the coming sunrise or sunset, in one pill. */
final class HoursWidget implements LockWidget {
    private static final long FADE_IN_MS = 1600;
    private static final int HOURS_SHOWN = 4;

    private final View pill;
    private final LinearLayout hours;
    private final TextView sun;
    private final LayoutInflater inflater;
    private final Fonts fonts;
    private final WeatherOutlook outlook;

    HoursWidget(LayoutInflater inflater, ViewGroup parent, Fonts fonts) {
        this.inflater = inflater;
        this.fonts = fonts;
        pill = inflater.inflate(R.layout.lock_widget_hours, parent, false);
        hours = pill.findViewById(R.id.lock_hours);
        sun = pill.findViewById(R.id.lock_sun);
        Resources res = pill.getResources();
        pill.setBackground(SurfaceDrawable.card(res, res.getDimension(R.dimen.lock_weather_height) / 2f));
        outlook = new WeatherOutlook(res);
    }

    @Override
    public View view() {
        return pill;
    }

    @Override
    public void onWeather(Weather weather) {
        hours.removeAllViews();
        int shown = Math.min(HOURS_SHOWN, weather.hours.size());
        for (int i = 0; i < shown; i++) {
            hours.addView(segmentFor(weather.hours.get(i), i == 0));
        }
        fonts.applyTo(hours);
        sun.setText(outlook.sunEvent(weather));
        pill.animate().alpha(1f).setDuration(FADE_IN_MS).start();
    }

    @Override
    public void onWeatherUnavailable() {
        pill.setVisibility(View.GONE);
    }

    private View segmentFor(Weather.Hour hour, boolean isNow) {
        Resources res = pill.getResources();
        View segment = inflater.inflate(R.layout.lock_hour_segment, hours, false);
        ImageView glyph = segment.findViewById(R.id.hour_segment_glyph);
        glyph.setImageDrawable(new WeatherGlyph(hour.condition, hour.daytime));
        String time = isNow ? res.getString(R.string.hour_now) : res.getString(R.string.hour_of_day, hour.hour);
        TextView text = segment.findViewById(R.id.hour_segment_text);
        text.setText(res.getString(R.string.lock_hour_temperature, time, hour.temperature));
        return segment;
    }
}
