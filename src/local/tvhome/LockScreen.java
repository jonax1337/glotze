package local.tvhome;

import android.content.Context;
import android.view.View;

import java.util.Random;

/**
 * The screen saver's face: aurora light, date, clock and widgets as styled in the editor; at night
 * just the dimmed clock. It fades in gently and drifts a few pixels every minute, so nothing stays
 * burnt into the panel.
 */
final class LockScreen {
    private static final long FADE_IN_MS = 1600;
    private static final long CONTENT_DELAY_MS = 400;
    private static final long HINT_VISIBLE_MS = 4000;
    private static final long HINT_FADE_MS = 900;
    private static final long DRIFT_EVERY_MS = 60_000;
    /** So slow that the eye never catches the content moving. */
    private static final long DRIFT_MS = 45_000;
    private static final float NIGHT_CONTENT_ALPHA = 0.35f;

    private final AuroraView aurora;
    private final View content;
    private final View hint;
    private final WeatherService weather;
    private final LockStyle style;
    private final ClockFace face;
    private final LockWidgets widgets;
    private final float driftRange;
    private final Random random = new Random();
    private final Runnable drift = this::drift;
    private final NightMode nightMode;
    private final MusicMood music;
    private boolean night;

    LockScreen(View root, Context context) {
        music = new MusicMood(context, () -> showLookForTimeOfDay(0));
        aurora = root.findViewById(R.id.lock_aurora);
        content = root.findViewById(R.id.lock_content);
        hint = root.findViewById(R.id.lock_hint);
        weather = new WeatherService(context);
        style = new LockStyle(context);
        face = new ClockFace(root);
        Fonts fonts = new Fonts(context.getAssets());
        fonts.applyTo(root);
        widgets = new LockWidgets(root.findViewById(R.id.lock_widgets), fonts, weather);
        nightMode = new NightMode(context);
        driftRange = context.getResources().getDimension(R.dimen.lock_drift);
    }

    void start() {
        applyStyle(CONTENT_DELAY_MS);
        hint.animate().alpha(1f).setDuration(FADE_IN_MS)
                .withEndAction(() -> hint.animate().alpha(0f)
                        .setStartDelay(HINT_VISIBLE_MS).setDuration(HINT_FADE_MS).start())
                .start();
        content.postDelayed(drift, DRIFT_EVERY_MS);
    }

    /** Shows the style as currently saved; the editor calls this after every change. */
    void applyStyle() {
        applyStyle(0);
    }

    private void applyStyle(long contentDelayMs) {
        night = nightMode.isActive();
        face.apply(style);
        widgets.show(style.widgets(), night);
        if (style.reactsToMusic()) {
            music.start();
        } else {
            music.stop();
        }
        showLookForTimeOfDay(contentDelayMs);
    }

    /** The music stops first: its end would otherwise light the backdrop again. */
    void stop() {
        music.stop();
        aurora.stop();
        widgets.stop();
        content.removeCallbacks(drift);
    }

    void release() {
        stop();
        weather.shutdown();
    }

    /**
     * Playing music makes the theme's light breathe with it. At night only a dimmed clock
     * remains: no light, everything at low brightness.
     */
    private void showLookForTimeOfDay(long contentDelayMs) {
        LockStyle.Background background = style.background();
        boolean lit = !night && LockBackdrop.isLit(background);
        aurora.setPulse(music.isPlaying() ? music.pulse() : Pulse.NONE);
        if (lit) {
            aurora.start(LockBackdrop.paletteOf(background));
        } else {
            aurora.stop();
        }
        aurora.animate().alpha(lit ? 1f : 0f).setDuration(FADE_IN_MS).start();
        content.animate().alpha(night ? NIGHT_CONTENT_ALPHA : 1f)
                .setStartDelay(contentDelayMs).setDuration(FADE_IN_MS).start();
    }

    /** Also follows the hour for the "time of day" background. */
    private void drift() {
        night = nightMode.isActive();
        widgets.setNight(night);
        showLookForTimeOfDay(0);
        content.animate()
                .translationX(randomOffset())
                .translationY(randomOffset())
                .setStartDelay(0)
                .setDuration(DRIFT_MS)
                .start();
        content.postDelayed(drift, DRIFT_EVERY_MS);
    }

    private float randomOffset() {
        return (random.nextFloat() * 2f - 1f) * driftRange;
    }
}
