package local.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;

/**
 * A surface in the current design. Soft Solid paints one flat, opaque fill without gradient, rim or
 * shadow, and marks the focus with a lime ring with a narrow gap in the ground colour inside it.
 * Glas paints frosted cards with a light rim, fills a focused row white and lays a sheen over a
 * focused tile. The theme decides all of it, see res/values/design_attrs.xml. A drawable cannot
 * paint outside its bounds, so every stroke sits just inside the edge.
 */
final class SurfaceDrawable extends Drawable {
    private enum Look {
        /** Bars, chips, cards and lists. */
        CARD(R.attr.cardColor, true, false),
        /** Menus, sheets and dialogs, one step lighter than the cards they float over. */
        RAISED(R.attr.raisedColor, true, false),
        /** The focused tile: the ring only, the artwork behind stays visible. */
        FOCUS_RING(NO_BODY, false, true),
        /** The focused row, button or key: the ring over the focus fill. */
        FOCUS_FILL(R.attr.focusFillColor, false, true);

        final int bodyColorAttr;
        final boolean hasRim;
        final boolean hasFocusMarks;

        Look(int bodyColorAttr, boolean hasRim, boolean hasFocusMarks) {
            this.bodyColorAttr = bodyColorAttr;
            this.hasRim = hasRim;
            this.hasFocusMarks = hasFocusMarks;
        }
    }

    private static final int NO_BODY = 0;
    private static final int OPAQUE = 255;
    /** The sheen fades out this far down the tile. */
    private static final float SHEEN_DEPTH = 0.6f;

    private final Look look;
    private final float cornerRadius;
    private final float rimWidth;
    private final float ringWidth;
    private final float ringGap;
    private final int bodyColor;
    private final int rimColor;
    private final int gapColor;
    private final int ringColor;
    private final int sheenColor;
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bodyShape = new RectF();
    private final RectF rimShape = new RectF();
    private final RectF gapShape = new RectF();
    private final RectF ringShape = new RectF();

    private SurfaceDrawable(Context context, Look look, float cornerRadius) {
        this.look = look;
        this.cornerRadius = cornerRadius;
        this.rimWidth = ThemeValues.dimension(context, R.attr.rimWidth);
        this.ringWidth = ThemeValues.dimension(context, R.attr.focusRingWidth);
        this.ringGap = ThemeValues.dimension(context, R.attr.focusRingGap);
        this.bodyColor = look.bodyColorAttr == NO_BODY ? Color.TRANSPARENT : ThemeValues.color(context, look.bodyColorAttr);
        this.rimColor = ThemeValues.color(context, R.attr.rimColor);
        this.gapColor = ThemeValues.color(context, R.attr.groundColor);
        this.ringColor = ThemeValues.color(context, R.attr.focusRingColor);
        this.sheenColor = ThemeValues.color(context, R.attr.focusSheenColor);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(rimWidth);
        gap.setStyle(Paint.Style.STROKE);
        gap.setStrokeWidth(ringGap);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(ringWidth);
        applyAlpha(OPAQUE);
    }

    static SurfaceDrawable card(Context context, float cornerRadius) {
        return new SurfaceDrawable(context, Look.CARD, cornerRadius);
    }

    static SurfaceDrawable raised(Context context, float cornerRadius) {
        return new SurfaceDrawable(context, Look.RAISED, cornerRadius);
    }

    /** A card that shows the focus while its view has focus. */
    static Drawable focusableCard(Context context, float cornerRadius) {
        return new LayerDrawable(new Drawable[] {
                card(context, cornerRadius), whileFocused(context, Look.FOCUS_FILL, cornerRadius)});
    }

    /** The focus ring over a tile's artwork while focused, nothing otherwise. */
    static StateListDrawable focusRing(Context context, float cornerRadius) {
        return whileFocused(context, Look.FOCUS_RING, cornerRadius);
    }

    /** Cross-fades between the focus look and nothing. */
    private static StateListDrawable whileFocused(Context context, Look look, float cornerRadius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, new SurfaceDrawable(context, look, cornerRadius));
        states.addState(new int[0], new ColorDrawable(Color.TRANSPARENT));
        int fadeMs = context.getResources().getInteger(R.integer.focus_in_ms);
        states.setEnterFadeDuration(fadeMs);
        states.setExitFadeDuration(fadeMs);
        return states;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        bodyShape.set(bounds);
        insetShape(rimShape, bounds, rimWidth / 2f);
        insetShape(gapShape, bounds, ringWidth + ringGap / 2f);
        insetShape(ringShape, bounds, ringWidth / 2f);
        sheen.setShader(new LinearGradient(0f, bounds.top, 0f, bounds.top + bounds.height() * SHEEN_DEPTH,
                sheenColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(bodyShape, cornerRadius, cornerRadius, body);
        if (look.hasRim) {
            drawStroke(canvas, rimShape, rimWidth / 2f, rim, rimWidth);
        }
        if (look.hasFocusMarks) {
            drawFocusMarks(canvas);
        }
    }

    private void drawFocusMarks(Canvas canvas) {
        if (Color.alpha(sheenColor) > 0) {
            canvas.drawRoundRect(bodyShape, cornerRadius, cornerRadius, sheen);
        }
        drawStroke(canvas, gapShape, ringWidth + ringGap / 2f, gap, ringGap);
        drawStroke(canvas, ringShape, ringWidth / 2f, ring, ringWidth);
    }

    /** A stroke of width zero would still paint a hairline, so a design without it skips it. */
    private void drawStroke(Canvas canvas, RectF shape, float inset, Paint paint, float width) {
        if (width > 0f) {
            float radius = Math.max(0f, cornerRadius - inset);
            canvas.drawRoundRect(shape, radius, radius, paint);
        }
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setRoundRect(getBounds(), cornerRadius);
        outline.setAlpha(0f); // surfaces cast no shadow, not even when a focused row is lifted
    }

    @Override
    public void setAlpha(int alpha) {
        applyAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        body.setColorFilter(colorFilter);
        rim.setColorFilter(colorFilter);
        gap.setColorFilter(colorFilter);
        ring.setColorFilter(colorFilter);
        sheen.setColorFilter(colorFilter);
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
        rim.setColor(withAlpha(rimColor, alpha));
        gap.setColor(withAlpha(gapColor, alpha));
        ring.setColor(withAlpha(ringColor, alpha));
        sheen.setAlpha(alpha);
    }

    private static void insetShape(RectF shape, Rect bounds, float inset) {
        shape.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Color.alpha(color) * alpha / OPAQUE, Color.red(color), Color.green(color), Color.blue(color));
    }
}
