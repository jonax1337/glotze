package local.tvhome;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/** A small chip with a weather symbol, the temperature, the condition and the place. Hidden without a forecast. */
final class WeatherChip implements WeatherService.Listener {
    private final View chip;
    private final ImageView glyph;
    private final TextView summary;
    private final Resources res;

    WeatherChip(View chip) {
        this.chip = chip;
        this.glyph = chip.findViewById(R.id.weather_chip_glyph);
        this.summary = chip.findViewById(R.id.weather_chip_text);
        this.res = chip.getResources();
        chip.setBackground(SurfaceDrawable.card(res, res.getDimension(R.dimen.stage_chip_corner)));
    }

    @Override
    public void onWeather(Weather weather) {
        glyph.setImageDrawable(new WeatherGlyph(weather.condition, weather.daytime));
        summary.setText(summaryOf(weather));
        chip.setVisibility(View.VISIBLE);
    }

    @Override
    public void onWeatherUnavailable() {
        chip.setVisibility(View.GONE);
    }

    private String summaryOf(Weather weather) {
        String condition = res.getString(weather.condition.labelRes);
        if (weather.place.isEmpty()) {
            return res.getString(R.string.stage_weather_chip_no_place, weather.temperature, condition);
        }
        return res.getString(R.string.stage_weather_chip, weather.temperature, condition, weather.place);
    }
}
