package local.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Glas: the Top Shelf of tvOS. The artwork of the app focused in the dock fills the top of the
 * screen edge to edge behind the clock, softened into light and dimmed so that the clock, the pill
 * and the chips stay readable, and fades into the dark ground towards the dock. The next app cross-fades in. Android apps offer no shelf
 * content of their own, so their banner, or their icon on its own colour, stands in for it. Every
 * artwork is exposed to the same dim brightness, so a white banner turns as dark as a black one,
 * and keeps its colour.
 */
public final class TopShelf extends View {
    /** The artwork is painted this many pixels wide, blurred and scaled up to the screen. */
    private static final int BACKDROP_WIDTH = 192;
    private static final int BLUR_RADIUS = 8;
    private static final int BLUR_PASSES = 3;
    /** The average brightness every artwork is dimmed to, from 0 to 255; brighter would drown white text. */
    private static final float TARGET_LUMA = 52f;
    /** Dimming greys colours, so they are made this much more vivid. */
    private static final float SATURATION = 1.6f;
    /** From here down, the artwork fades into the ground. */
    private static final float FADE_START = 0.3f;
    /** An icon covers this share of the shelf's height, on a fill of its own colour. */
    private static final float ICON_SHARE = 0.7f;
    private static final int OPAQUE = 255;

    private final Paint artwork = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint fade = new Paint();
    private final ValueAnimator crossFade = ValueAnimator.ofFloat(0f, 1f);
    private final Rect screen = new Rect();
    private final int groundColor;
    private Bitmap shown;
    private Bitmap leaving;
    private AppTile wanted;

    public TopShelf(Context context, AttributeSet attrs) {
        super(context, attrs);
        groundColor = ThemeValues.color(context, R.attr.groundColor);
        crossFade.setDuration(getResources().getInteger(R.integer.top_shelf_fade_ms));
        crossFade.addUpdateListener(animation -> invalidate());
    }

    /** Fills the shelf with the app's artwork, or will once the shelf knows its size. */
    void show(AppTile tile) {
        if (getVisibility() != VISIBLE || isWanted(tile)) {
            return;
        }
        wanted = tile;
        if (getWidth() > 0) {
            crossFadeTo(backdropOf(tile));
        }
    }

    private boolean isWanted(AppTile tile) {
        return wanted != null && wanted.key.equals(tile.key);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        screen.set(0, 0, width, height);
        fade.setShader(new LinearGradient(0f, height * FADE_START, 0f, height,
                Color.TRANSPARENT, groundColor, Shader.TileMode.CLAMP));
        if (wanted != null) {
            crossFadeTo(backdropOf(wanted));
        }
    }

    private void crossFadeTo(Bitmap backdrop) {
        leaving = shown;
        shown = backdrop;
        crossFade.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (shown == null) {
            return;
        }
        artwork.setAlpha(OPAQUE);
        if (leaving != null && crossFade.isRunning()) {
            canvas.drawBitmap(leaving, null, screen, artwork);
            artwork.setAlpha(Math.round(OPAQUE * (float) crossFade.getAnimatedValue()));
        }
        canvas.drawBitmap(shown, null, screen, artwork);
        canvas.drawRect(screen, fade);
    }

    /** The artwork, small and blurred, in the shape of the shelf. */
    private Bitmap backdropOf(AppTile tile) {
        int width = BACKDROP_WIDTH;
        int height = Math.max(1, Math.round(width * getHeight() / (float) getWidth()));
        Bitmap backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(backdrop);
        canvas.drawColor(groundColor);
        if (tile.hasBanner) {
            drawWithin(canvas, tile.artwork, cropped(tile.artwork, width, height));
        } else {
            drawWithin(canvas, ArtworkSampler.iconBackground(tile.artwork, 0f), new Rect(0, 0, width, height));
            int size = Math.round(height * ICON_SHARE);
            int left = (width - size) / 2;
            int top = (height - size) / 2;
            drawWithin(canvas, tile.artwork, new Rect(left, top, left + size, top + size));
        }
        soften(backdrop);
        return backdrop;
    }

    /** Where the banner must go to cover the whole shelf without distortion, cut evenly on two sides. */
    private static Rect cropped(Drawable banner, int width, int height) {
        int bannerWidth = Math.max(1, banner.getIntrinsicWidth());
        int bannerHeight = Math.max(1, banner.getIntrinsicHeight());
        float scale = Math.max(width / (float) bannerWidth, height / (float) bannerHeight);
        int scaledWidth = Math.round(bannerWidth * scale);
        int scaledHeight = Math.round(bannerHeight * scale);
        int left = (width - scaledWidth) / 2;
        int top = (height - scaledHeight) / 2;
        return new Rect(left, top, left + scaledWidth, top + scaledHeight);
    }

    /** Draws without disturbing the bounds the tiles rely on. */
    private static void drawWithin(Canvas canvas, Drawable drawable, Rect bounds) {
        Rect originalBounds = drawable.copyBounds();
        drawable.setBounds(bounds);
        drawable.draw(canvas);
        drawable.setBounds(originalBounds);
    }

    /** Blurs the artwork, then dims it to the common brightness and makes its colours more vivid. */
    private static void soften(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        int[] turned = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int pass = 0; pass < BLUR_PASSES; pass++) {
            blurRowsIntoColumns(pixels, turned, width, height);
            blurRowsIntoColumns(turned, pixels, height, width);
        }
        float exposure = Math.min(1f, TARGET_LUMA / Math.max(1f, averageLuma(pixels)));
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = exposed(pixels[i], exposure);
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private static float averageLuma(int[] pixels) {
        float sum = 0f;
        for (int color : pixels) {
            sum += luma(color);
        }
        return sum / pixels.length;
    }

    private static float luma(int color) {
        return 0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color);
    }

    /** Pushes each channel away from the grey of the same brightness, then scales it down. */
    private static int exposed(int color, float exposure) {
        float grey = luma(color);
        return Color.rgb(channel(Color.red(color), grey, exposure), channel(Color.green(color), grey, exposure),
                channel(Color.blue(color), grey, exposure));
    }

    private static int channel(int value, float grey, float exposure) {
        float vivid = grey + (value - grey) * SATURATION;
        return Math.max(0, Math.min(OPAQUE, Math.round(vivid * exposure)));
    }

    /** Box-blurs each row and writes it as a column, so two calls blur both ways and turn the picture back. */
    private static void blurRowsIntoColumns(int[] from, int[] to, int width, int height) {
        int span = 2 * BLUR_RADIUS + 1;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int alpha = 0;
                int red = 0;
                int green = 0;
                int blue = 0;
                for (int k = -BLUR_RADIUS; k <= BLUR_RADIUS; k++) {
                    int color = from[row + Math.min(width - 1, Math.max(0, x + k))];
                    alpha += Color.alpha(color);
                    red += Color.red(color);
                    green += Color.green(color);
                    blue += Color.blue(color);
                }
                to[x * height + y] = Color.argb(alpha / span, red / span, green / span, blue / span);
            }
        }
    }
}
