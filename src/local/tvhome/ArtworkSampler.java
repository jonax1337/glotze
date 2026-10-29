package local.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/** Derives an icon-only app's tile colour from its icon. */
final class ArtworkSampler {
    private static final int ACCENT_SAMPLE_SIZE = 12;
    private static final int OPAQUE_ENOUGH = 200;
    private static final float MAX_ACCENT_SATURATION = 0.75f;
    private static final float MAX_ACCENT_BRIGHTNESS = 0.5f;
    private static final float[] NEUTRAL_HSV = {220f, 0.2f, 0.3f};

    private ArtworkSampler() {
    }

    /** A flat fill in the icon's most vivid colour, so icon-only apps still feel rich. */
    static GradientDrawable iconBackground(Drawable icon, float cornerRadius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.HSVToColor(accentHsv(icon)));
        background.setCornerRadius(cornerRadius);
        return background;
    }

    private static float[] accentHsv(Drawable icon) {
        Bitmap sample = render(icon, ACCENT_SAMPLE_SIZE, ACCENT_SAMPLE_SIZE);
        float[] best = NEUTRAL_HSV.clone();
        float bestVividness = 0f;
        float[] hsv = new float[3];
        for (int y = 0; y < ACCENT_SAMPLE_SIZE; y++) {
            for (int x = 0; x < ACCENT_SAMPLE_SIZE; x++) {
                int pixel = sample.getPixel(x, y);
                Color.colorToHSV(pixel, hsv);
                float vividness = hsv[1] * hsv[2];
                if (Color.alpha(pixel) > OPAQUE_ENOUGH && vividness > bestVividness) {
                    bestVividness = vividness;
                    best = hsv.clone();
                }
            }
        }
        best[1] = Math.min(best[1], MAX_ACCENT_SATURATION);
        best[2] = Math.min(best[2], MAX_ACCENT_BRIGHTNESS);
        return best;
    }

    /** Draws the drawable at the given size without disturbing the bounds its views rely on. */
    private static Bitmap render(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Rect originalBounds = drawable.copyBounds();
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(bitmap));
        drawable.setBounds(originalBounds);
        return bitmap;
    }
}
