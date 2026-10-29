package local.tvhome;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;

/**
 * A "Soft Solid" surface: one flat, opaque fill without gradient, rim or shadow. The only
 * decoration is the focus ring, a Glotze-lime stroke with a narrow gap in the ground colour
 * inside it. A drawable cannot paint outside its bounds, so both strokes sit just inside the edge.
 */
final class SurfaceDrawable extends Drawable {
    private enum Look {
        /** Bars, chips, cards and lists. */
        CARD(R.color.surface_card, false),
        /** Menus, sheets and dialogs, one step lighter than the cards they float over. */
        RAISED(R.color.surface_card_raised, false),
        /** The focused element: the ring only, whatever lies behind stays visible. */
        FOCUS_RING(android.R.color.transparent, true);

        final int bodyColorRes;
        final boolean hasRing;

        Look(int bodyColorRes, boolean hasRing) {
            this.bodyColorRes = bodyColorRes;
            this.hasRing = hasRing;
        }
    }

    private static final int OPAQUE = 255;

    private final Look look;
    private final float cornerRadius;
    private final float ringWidth;
    private final float ringGap;
    private final int bodyColor;
    private final int gapColor;
    private final int ringColor;
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bodyShape = new RectF();
    private final RectF gapShape = new RectF();
    private final RectF ringShape = new RectF();

    private SurfaceDrawable(Resources res, Look look, float cornerRadius) {
        this.look = look;
        this.cornerRadius = cornerRadius;
        this.ringWidth = res.getDimension(R.dimen.surface_ring_width);
        this.ringGap = res.getDimension(R.dimen.surface_ring_gap);
        this.bodyColor = res.getColor(look.bodyColorRes, null);
        this.gapColor = res.getColor(R.color.surface_ground, null);
        this.ringColor = res.getColor(R.color.glotze_lime, null);
        gap.setStyle(Paint.Style.STROKE);
        gap.setStrokeWidth(ringGap);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(ringWidth);
        applyAlpha(OPAQUE);
    }

    static SurfaceDrawable card(Resources res, float cornerRadius) {
        return new SurfaceDrawable(res, Look.CARD, cornerRadius);
    }

    static SurfaceDrawable raised(Resources res, float cornerRadius) {
        return new SurfaceDrawable(res, Look.RAISED, cornerRadius);
    }

    /** A card that gains the focus ring while its view has focus. */
    static Drawable focusableCard(Resources res, float cornerRadius) {
        return new LayerDrawable(new Drawable[] {card(res, cornerRadius), focusRing(res, cornerRadius)});
    }

    /** Lime focus ring while focused, nothing otherwise, cross-fading between the two. */
    static StateListDrawable focusRing(Resources res, float cornerRadius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused},
                new SurfaceDrawable(res, Look.FOCUS_RING, cornerRadius));
        states.addState(new int[0], new ColorDrawable(Color.TRANSPARENT));
        int fadeMs = res.getInteger(R.integer.focus_in_ms);
        states.setEnterFadeDuration(fadeMs);
        states.setExitFadeDuration(fadeMs);
        return states;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        bodyShape.set(bounds);
        insetShape(gapShape, bounds, ringWidth + ringGap / 2f);
        insetShape(ringShape, bounds, ringWidth / 2f);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(bodyShape, cornerRadius, cornerRadius, body);
        if (look.hasRing) {
            float gapRadius = Math.max(0f, cornerRadius - ringWidth - ringGap / 2f);
            float ringRadius = Math.max(0f, cornerRadius - ringWidth / 2f);
            canvas.drawRoundRect(gapShape, gapRadius, gapRadius, gap);
            canvas.drawRoundRect(ringShape, ringRadius, ringRadius, ring);
        }
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setRoundRect(getBounds(), cornerRadius);
        outline.setAlpha(0f); // Soft Solid casts no shadow, not even when a focused row is lifted
    }

    @Override
    public void setAlpha(int alpha) {
        applyAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        body.setColorFilter(colorFilter);
        gap.setColorFilter(colorFilter);
        ring.setColorFilter(colorFilter);
        invalidateSelf();
    }

    /** Deprecated since API 29 but still abstract, so every Drawable has to implement it. */
    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /** Scales each colour's own alpha instead of replacing it, so a transparent fill stays transparent. */
    private void applyAlpha(int alpha) {
        body.setColor(withAlpha(bodyColor, alpha));
        gap.setColor(withAlpha(gapColor, alpha));
        ring.setColor(withAlpha(ringColor, alpha));
    }

    private static void insetShape(RectF shape, Rect bounds, float inset) {
        shape.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Color.alpha(color) * alpha / OPAQUE, Color.red(color), Color.green(color), Color.blue(color));
    }
}
