package local.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * A band of light that sweeps once across a tile as it takes the focus, from the top left to the
 * bottom right, as if the lifted glass turned towards a lamp. It paints nothing while at rest.
 */
final class FocusShine extends Drawable {
    /** The band is this share of the tile's width. */
    private static final float BAND_SHARE = 0.5f;
    /** The band leans this far: it travels half a step down for every step to the right. */
    private static final float SLOPE = 0.5f;
    /** Lets the tile begin to lift before the light reaches it. */
    private static final long DELAY_MS = 90;

    private final Paint paint = new Paint();
    private final int color;
    private final ValueAnimator sweep = ValueAnimator.ofFloat(0f, 1f);
    private final Matrix position = new Matrix();
    private float travel;
    private float bandWidth;

    private FocusShine(Context context, int color) {
        this.color = color;
        sweep.setDuration(context.getResources().getInteger(R.integer.focus_shine_ms));
        sweep.setStartDelay(DELAY_MS);
        sweep.setInterpolator(new AccelerateDecelerateInterpolator());
        sweep.addUpdateListener(animation -> invalidateSelf());
    }

    /** The shine of the current design; one whose colour is transparent never shows. */
    static FocusShine of(Context context) {
        return new FocusShine(context, ThemeValues.color(context, R.attr.focusShineColor));
    }

    void play() {
        if (Color.alpha(color) > 0) {
            sweep.start();
        }
    }

    void stop() {
        sweep.cancel();
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        bandWidth = bounds.width() * BAND_SHARE;
        // From fully left of the tile to fully past its bottom right corner.
        travel = bounds.width() + bounds.height() * SLOPE + bandWidth;
        paint.setShader(new LinearGradient(0f, 0f, bandWidth, bandWidth * SLOPE,
                new int[] {Color.TRANSPARENT, color, Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        if (!sweep.isRunning()) {
            return;
        }
        Rect bounds = getBounds();
        float offset = bounds.left - bandWidth + travel * (float) sweep.getAnimatedValue();
        position.setTranslate(offset, bounds.top);
        paint.getShader().setLocalMatrix(position);
        canvas.drawRect(bounds, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    /** Deprecated since API 29 but still abstract, so every Drawable has to implement it. */
    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
