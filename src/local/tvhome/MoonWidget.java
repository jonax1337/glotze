package local.tvhome;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

/** Tonight's moon and the time in a second city. */
final class MoonWidget implements LockWidget {
    private static final int PERCENT = 100;

    private final View pill;

    MoonWidget(LayoutInflater inflater, ViewGroup parent) {
        pill = inflater.inflate(R.layout.lock_widget_moon, parent, false);
        Resources res = pill.getResources();
        pill.setBackground(SurfaceDrawable.card(pill.getContext(), res.getDimension(R.dimen.lock_weather_height) / 2f));
        PlaceSettings.City city = new PlaceSettings(pill.getContext()).worldClock();
        TextView cityName = pill.findViewById(R.id.world_clock_city);
        cityName.setText(city.name);
        TextClock worldClock = pill.findViewById(R.id.world_clock);
        worldClock.setTimeZone(city.zoneId);
    }

    @Override
    public View view() {
        return pill;
    }

    /** The phase barely changes in an evening, so it is read once when the screen saver starts. */
    @Override
    public void start() {
        MoonPhase phase = MoonPhase.at(System.currentTimeMillis());
        ImageView glyph = pill.findViewById(R.id.moon_glyph);
        glyph.setImageDrawable(new MoonGlyph(phase));
        TextView name = pill.findViewById(R.id.moon_phase);
        name.setText(pill.getResources().getString(R.string.moon_phase,
                pill.getResources().getString(phase.nameRes()),
                Math.round(phase.illumination() * PERCENT)));
    }
}
