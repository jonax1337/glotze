package local.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextClock;

/**
 * The big clock on the stage. In Glas its digits are glass, like the clock of the iPhone lock screen:
 * white at the top, letting the light behind through towards the bottom, with a bright edge where the
 * light falls on them. In Soft Solid both colours of the theme are the text colour, so it stays flat.
 */
public final class StageClock extends TextClock {
    /** The bright edge fades out this far down the digits. */
    private static final float EDGE_DEPTH = 0.55f;

    private final int shadeColor;
    private final int edgeColor;
    private final float edgeWidth;
    private Shader fill;
    private Shader edge;

    public StageClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        shadeColor = ThemeValues.color(context, R.attr.clockShadeColor);
        edgeColor = ThemeValues.color(context, R.attr.clockEdgeColor);
        edgeWidth = getResources().getDimension(R.dimen.glass_clock_edge);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        fill = new LinearGradient(0f, 0f, 0f, height, getCurrentTextColor(), shadeColor, Shader.TileMode.CLAMP);
        edge = new LinearGradient(0f, 0f, 0f, height * EDGE_DEPTH, edgeColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        TextPaint paint = getPaint();
        paint.setShader(fill);
        super.onDraw(canvas);
        if (Color.alpha(edgeColor) > 0) {
            drawEdge(canvas, paint);
        }
        paint.setShader(null);
    }

    /** Draws the digits again as an outline only. */
    private void drawEdge(Canvas canvas, TextPaint paint) {
        paint.setShader(edge);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(edgeWidth);
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
    }
}
