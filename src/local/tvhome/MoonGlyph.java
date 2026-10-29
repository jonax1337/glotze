package local.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * The moon as it looks tonight: a faint disc with its lit part on top. The path is built in
 * pixels, because Android 7 rasterises paths before scaling and loses small unit-square ones.
 */
final class MoonGlyph extends Drawable {
    private static final int DARK_SIDE = 0x2EFFFFFF;
    private static final int LIT_SIDE = 0xF0F4F1E6;
    private static final float TOP = -90f;
    private static final float BOTTOM = 90f;
    private static final float HALF_TURN = 180f;
    private static final double QUARTER = 0.25;

    private final MoonPhase phase;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    MoonGlyph(MoonPhase phase) {
        this.phase = phase;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float radius = Math.min(bounds.width(), bounds.height()) / 2f;
        float centerX = bounds.exactCenterX();
        float centerY = bounds.exactCenterY();
        paint.setColor(DARK_SIDE);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setColor(LIT_SIDE);
        canvas.drawPath(litPart(centerX, centerY, radius), paint);
    }

    /**
     * Half the disc on the lit side, closed by the terminator: an ellipse whose half-width shrinks
     * to nothing at the quarters. A crescent's terminator bulges towards the lit side, a gibbous
     * moon's away from it.
     */
    private Path litPart(float centerX, float centerY, float radius) {
        double waxingAge = phase.isWaxing() ? phase.age : 1 - phase.age;
        float terminator = (float) Math.abs(Math.cos(2 * Math.PI * waxingAge)) * radius;
        float side = phase.isWaxing() ? 1f : -1f;
        boolean crescent = waxingAge < QUARTER;
        Path lit = new Path();
        lit.arcTo(new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                TOP, HALF_TURN * side);
        lit.arcTo(new RectF(centerX - terminator, centerY - radius, centerX + terminator, centerY + radius),
                BOTTOM, (crescent ? -HALF_TURN : HALF_TURN) * side);
        lit.close();
        return lit;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
