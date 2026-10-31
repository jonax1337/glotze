package local.tvhome;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/** The weather in one line: glyph, temperature, condition and place. */
final class WeatherPillWidget implements LockWidget {
    private static final long FADE_IN_MS = 1600;

    private final View pill;
    private final ImageView glyph;
    private final TextView text;

    WeatherPillWidget(LayoutInflater inflater, ViewGroup parent) {
        pill = inflater.inflate(R.layout.lock_widget_weather, parent, false);
        glyph = pill.findViewById(R.id.lock_weather_glyph);
        text = pill.findViewById(R.id.lock_weather_text);
        Resources res = pill.getResources();
        pill.setBackground(SurfaceDrawable.card(pill.getContext(), res.getDimension(R.dimen.lock_weather_height) / 2f));
    }

    @Override
    public View view() {
        return pill;
    }

    @Override
    public void onWeather(Weather now) {
        glyph.setImageDrawable(new WeatherGlyph(now.condition, now.daytime));
        text.setText(pill.getResources().getString(R.string.lock_weather, now.temperature,
                pill.getResources().getString(now.condition.labelRes), now.place));
        pill.animate().alpha(1f).setDuration(FADE_IN_MS).start();
    }

    @Override
    public void onWeatherUnavailable() {
        pill.setVisibility(View.GONE);
    }
}
