package local.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * A soft, rounded weather symbol drawn in a unit square: sun or moon, clouds, and whatever
 * falls from them. Drawn rather than bundled so every condition shares one visual language.
 */
final class WeatherGlyph extends Drawable {
    private static final int SUN = 0xFFFFC857;
    private static final int MOON = 0xFFE6ECF7;
    private static final int CLOUD = 0xFFF4F7FB;
    private static final int RAIN = 0xFF7CC6FF;
    private static final int SNOW = 0xFFFFFFFF;
    private static final int BOLT = 0xFFFFD84D;
    private static final int FOG = 0xCCF4F7FB;
    private static final float LINE = 0.055f;
    private static final int SUN_RAYS = 8;

    private final WeatherCondition condition;
    private final boolean daytime;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pixelsPerUnit = 1f;

    WeatherGlyph(WeatherCondition condition, boolean daytime) {
        this.condition = condition;
        this.daytime = daytime;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(LINE);    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float size = Math.min(bounds.width(), bounds.height());
        pixelsPerUnit = size;
        canvas.save();
        canvas.translate(bounds.exactCenterX() - size / 2f, bounds.exactCenterY() - size / 2f);
        canvas.scale(size, size);
        drawCondition(canvas);
        canvas.restore();
    }

    /**
     * Android 7 rasterises a path at the size of its own coordinates before scaling it, so a
     * path in the unit square would come out under one pixel and vanish. Circles and lines are
     * unaffected; paths are therefore drawn in pixels.
     */
    private void drawPathInPixels(Canvas canvas, Path unitPath) {
        Matrix toPixels = new Matrix();
        toPixels.setScale(pixelsPerUnit, pixelsPerUnit);
        Path pixelPath = new Path();
        unitPath.transform(toPixels, pixelPath);
        canvas.save();
        canvas.scale(1f / pixelsPerUnit, 1f / pixelsPerUnit);
        canvas.drawPath(pixelPath, fill);
        canvas.restore();
    }

    private void drawCondition(Canvas canvas) {
        switch (condition) {
            case CLEAR:
                drawSky(canvas, 0.5f, 0.5f, 0.2f);
                break;
            case PARTLY_CLOUDY:
                // Sun or moon well clear of the cloud, so "partly" still reads as partly at night.
                drawSky(canvas, 0.34f, 0.32f, 0.17f);
                drawCloud(canvas, 0.6f, 0.66f, 0.7f);
                break;
            case FOG:
                drawFog(canvas);
                break;
            case DRIZZLE:
                drawCloud(canvas, 0.5f, 0.44f, 0.9f);
                drawStreaks(canvas, 0.04f);
                break;
            case RAIN:
                drawCloud(canvas, 0.5f, 0.44f, 0.9f);
                drawStreaks(canvas, 0.1f);
                break;
            case SNOW:
                drawCloud(canvas, 0.5f, 0.44f, 0.9f);
                drawFlakes(canvas);
                break;
            case THUNDER:
                drawCloud(canvas, 0.5f, 0.44f, 0.9f);
                drawBolt(canvas);
                break;
            default:
                drawCloud(canvas, 0.5f, 0.54f, 1f);
                break;
        }
    }

    private void drawSky(Canvas canvas, float cx, float cy, float radius) {
        if (daytime) {
            drawSun(canvas, cx, cy, radius);
        } else {
            drawMoon(canvas, cx, cy, radius * 1.15f);
        }
    }

    private void drawSun(Canvas canvas, float cx, float cy, float radius) {
        fill.setColor(SUN);
        canvas.drawCircle(cx, cy, radius, fill);
        line.setColor(SUN);
        for (int ray = 0; ray < SUN_RAYS; ray++) {
            double angle = 2 * Math.PI * ray / SUN_RAYS;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            canvas.drawLine(cx + cos * radius * 1.45f, cy + sin * radius * 1.45f,
                    cx + cos * radius * 1.85f, cy + sin * radius * 1.85f, line);
        }
    }

    /**
     * A crescent: the moon's disc minus a shadow disc offset towards the upper right, traced
     * as two arcs that meet where the circles cross.
     */
    private void drawMoon(Canvas canvas, float cx, float cy, float radius) {
        float dx = radius * 0.5f;
        float dy = -radius * 0.35f;
        float shadowRadius = radius * 0.85f;
        double distance = Math.hypot(dx, dy);
        // Along the line between the centres: how far the chord through both crossings lies.
        double chord = (radius * radius - shadowRadius * shadowRadius + distance * distance) / (2 * distance);
        double halfChord = Math.sqrt(radius * radius - chord * chord);
        double towardsShadow = Math.toDegrees(Math.atan2(dy, dx));
        double moonHalfAngle = Math.toDegrees(Math.atan2(halfChord, chord));
        double shadowHalfAngle = Math.toDegrees(Math.atan2(halfChord, distance - chord));
        Path crescent = new Path();
        crescent.arcTo(new RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                (float) (towardsShadow + moonHalfAngle), (float) (360 - 2 * moonHalfAngle), true);
        crescent.arcTo(new RectF(cx + dx - shadowRadius, cy + dy - shadowRadius,
                        cx + dx + shadowRadius, cy + dy + shadowRadius),
                (float) (towardsShadow + 180 + shadowHalfAngle), (float) (-2 * shadowHalfAngle), false);
        crescent.close();
        fill.setColor(MOON);
        drawPathInPixels(canvas, crescent);
    }

    /** A cloud built from three puffs on a rounded base; scale 1 spans most of the square. */
    private void drawCloud(Canvas canvas, float cx, float cy, float scale) {
        fill.setColor(CLOUD);
        canvas.drawCircle(cx - 0.17f * scale, cy, 0.14f * scale, fill);
        canvas.drawCircle(cx + 0.02f * scale, cy - 0.08f * scale, 0.2f * scale, fill);
        canvas.drawCircle(cx + 0.2f * scale, cy + 0.02f * scale, 0.13f * scale, fill);
        canvas.drawRoundRect(cx - 0.31f * scale, cy, cx + 0.33f * scale, cy + 0.15f * scale,
                0.075f * scale, 0.075f * scale, fill);
    }

    private void drawStreaks(Canvas canvas, float length) {
        line.setColor(RAIN);
        for (float x = 0.36f; x <= 0.66f; x += 0.15f) {
            canvas.drawLine(x, 0.72f, x - length * 0.4f, 0.72f + length, line);
        }
    }

    private void drawFlakes(Canvas canvas) {
        fill.setColor(SNOW);
        for (float x = 0.36f; x <= 0.66f; x += 0.15f) {
            canvas.drawCircle(x, 0.76f, 0.035f, fill);
        }
    }

    private void drawBolt(Canvas canvas) {
        Path bolt = new Path();
        bolt.moveTo(0.53f, 0.64f);
        bolt.lineTo(0.42f, 0.8f);
        bolt.lineTo(0.5f, 0.8f);
        bolt.lineTo(0.44f, 0.95f);
        bolt.lineTo(0.61f, 0.75f);
        bolt.lineTo(0.53f, 0.75f);
        bolt.lineTo(0.59f, 0.64f);
        bolt.close();
        fill.setColor(BOLT);
        drawPathInPixels(canvas, bolt);
    }

    private void drawFog(Canvas canvas) {
        line.setColor(FOG);
        canvas.drawLine(0.22f, 0.4f, 0.78f, 0.4f, line);
        canvas.drawLine(0.16f, 0.55f, 0.7f, 0.55f, line);
        canvas.drawLine(0.28f, 0.7f, 0.84f, 0.7f, line);
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        line.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
        line.setColorFilter(colorFilter);
        invalidateSelf();
    }

    /** Deprecated since API 29 but still abstract, so every Drawable has to implement it. */
    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
