package local.tvhome;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/** Glotze in a pill: awake by day, nodding off when the night begins and asleep until it ends. */
final class GlotzeWidget implements LockWidget {
    private final View pill;
    private final GlotzeView glotze;

    GlotzeWidget(LayoutInflater inflater, ViewGroup parent) {
        pill = inflater.inflate(R.layout.lock_widget_glotze, parent, false);
        glotze = pill.findViewById(R.id.lock_glotze);
        Resources res = pill.getResources();
        pill.setBackground(SurfaceDrawable.card(pill.getContext(), res.getDimension(R.dimen.lock_weather_height) / 2f));
    }

    @Override
    public View view() {
        return pill;
    }

    /** Asleep Glotze keeps still after dozing off once, so the night costs the screen nothing. */
    @Override
    public void onNight(boolean night) {
        glotze.setAsleep(night);
        if (night) {
            glotze.play(GlotzeView.Motion.DOZE);
        } else {
            glotze.loop(GlotzeView.Motion.IDLE);
        }
    }

    @Override
    public void stop() {
        glotze.stop();
    }
}
