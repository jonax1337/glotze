package local.tvhome;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ScrollView;

/**
 * A scroll view that glides to the focused item. The stock ScrollView jumps instantly when
 * focus moves, and its own smooth scroll snaps when keys repeat quickly; this one always eases.
 */
public final class FocusScrollView extends ScrollView {
    private static final long GLIDE_MS = 560;
    private static final Interpolator EASE_OUT = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final int revealMargin;
    private ObjectAnimator glide;

    public FocusScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        revealMargin = getResources().getDimensionPixelSize(R.dimen.reveal_margin);
    }

    void glideTo(int scrollY) {
        if (glide != null) {
            glide.cancel();
        }
        glide = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), scrollY).setDuration(GLIDE_MS);
        glide.setInterpolator(EASE_OUT);
        glide.start();
    }

    /** Scrolls just far enough that the view, plus a margin for its focus lift, is on screen. */
    void glideToReveal(View descendant) {
        glideToReveal(descendant, 0);
    }

    /** Like {@link #glideToReveal(View)}, but never stops above {@code minScrollY}. */
    void glideToReveal(View descendant, int minScrollY) {
        Rect area = new Rect();
        descendant.getDrawingRect(area);
        offsetDescendantRectToMyCoords(descendant, area);
        area.inset(0, -revealMargin);
        int revealed = getScrollY() + super.computeScrollDeltaToGetChildRectOnScreen(area);
        glideTo(Math.max(minScrollY, revealed));
    }

    /** Disables the built-in instant jump; scrolling is driven by {@link #glideToReveal}. */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return 0;
    }
}
