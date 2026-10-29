package local.tvhome;

import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

/** Like tvOS at the edge of the screen: the card nudges towards where focus cannot go, then springs back. */
final class EdgeBump {
    private static final long BUMP_OUT_MS = 90;
    private static final long BUMP_BACK_MS = 320;
    private static final Interpolator GLIDE = new PathInterpolator(0.25f, 0.1f, 0.25f, 1f);
    private static final Interpolator SPRING_BACK = new OvershootInterpolator(2.5f);

    private EdgeBump() {
    }

    /** Nudges the card of the focused tile, or does nothing for a view without one, such as the settings button. */
    static void play(View focused, int focusDirection) {
        View card = focused.findViewById(R.id.card);
        if (card == null) {
            return;
        }
        float distance = focused.getResources().getDimension(R.dimen.tile_bump);
        float dx = focusDirection == View.FOCUS_LEFT ? -distance : focusDirection == View.FOCUS_RIGHT ? distance : 0f;
        float dy = focusDirection == View.FOCUS_UP ? -distance : focusDirection == View.FOCUS_DOWN ? distance : 0f;
        card.animate().cancel();
        card.animate()
                .translationX(dx)
                .translationY(dy)
                .setDuration(BUMP_OUT_MS)
                .setInterpolator(GLIDE)
                .withEndAction(() -> settle(card))
                .start();
    }

    private static void settle(View card) {
        card.animate()
                .translationX(0f)
                .translationY(0f)
                .setDuration(BUMP_BACK_MS)
                .setInterpolator(SPRING_BACK)
                .start();
    }
}
