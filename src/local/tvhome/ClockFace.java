package local.tvhome;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;

/** Lays out the lock screen's date, clock and widget row the way the chosen style says. */
final class ClockFace {
    private static final String CLOCK_FORMAT = "HH:mm";
    /** Hours above minutes, like the stacked clock on iOS. */
    private static final String STACKED_FORMAT = "HH'\n'mm";
    private static final float STACKED_LINE_SPACING = 0.8f;
    private static final String TABULAR_DIGITS = "tnum";
    /** The clock never shrinks below this share of its size; a lower limit than that is not worth a big clock. */
    private static final float MIN_CLOCK_SHARE = 0.45f;
    private static final float OVERHANG_SHARE = 0.12f;

    private final Resources res;
    private final LinearLayout content;
    private final TextClock date;
    private final TextClock clock;
    private final LinearLayout widgets;
    /** The size the layout asks for; fitClock may draw the clock smaller. */
    private float wantedClockSize;

    ClockFace(View root) {
        res = root.getResources();
        content = root.findViewById(R.id.lock_content);
        date = root.findViewById(R.id.lock_date);
        clock = root.findViewById(R.id.lock_clock);
        widgets = root.findViewById(R.id.lock_widgets);
        clock.setFontFeatureSettings(TABULAR_DIGITS);
        // Widgets fill in later (weather, departures), so the fit is checked on every layout.
        content.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                fitClock());
    }

    /**
     * A big clock with two tall widgets can outgrow the screen; then the clock alone gets smaller,
     * just enough for everything to fit, so the widgets stay readable.
     */
    private void fitClock() {
        View screen = (View) content.getParent();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
        int room = screen.getHeight() - params.topMargin - params.bottomMargin
                - res.getDimensionPixelSize(R.dimen.lock_corner_margin);
        int laidOutWidth = content.getWidth();
        int laidOutHeight = content.getHeight();
        content.measure(View.MeasureSpec.makeMeasureSpec(screen.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int clockHeight = Math.max(1, clock.getMeasuredHeight());
        int everythingElse = content.getMeasuredHeight() - clockHeight;
        // Measuring rebuilds the text layouts; put them back to the size actually on screen.
        content.measure(View.MeasureSpec.makeMeasureSpec(laidOutWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(laidOutHeight, View.MeasureSpec.EXACTLY));
        // The clock's height grows in step with its text size, so the size that fits follows directly.
        float sizePerPixel = clock.getTextSize() / clockHeight;
        float fitting = (room - everythingElse) * sizePerPixel;
        float size = Math.max(wantedClockSize * MIN_CLOCK_SHARE, Math.min(wantedClockSize, fitting));
        if (Math.abs(size - clock.getTextSize()) > 1f) {
            clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }

    void apply(LockStyle style) {
        LockStyle.Layout layout = style.layout();
        place(layout);
        clock.setTypeface(Typeface.createFromAsset(res.getAssets(), style.clockFontAsset()));
        clock.setTextColor(style.clockColour());
        date.setTextColor(style.clockColour());
        String format = layout == LockStyle.Layout.STACKED ? STACKED_FORMAT : CLOCK_FORMAT;
        clock.setFormat24Hour(format);
        clock.setFormat12Hour(format);
        clock.setLineSpacing(0f, layout == LockStyle.Layout.STACKED ? STACKED_LINE_SPACING : 1f);
        wantedClockSize = res.getDimension(clockSizeRes(layout));
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, wantedClockSize);
        // Wide and heavy cuts reach past their advance width, and the glow needs room too.
        int overhang = Math.round(wantedClockSize * OVERHANG_SHARE);
        clock.setPadding(overhang, 0, overhang, 0);
    }

    private void place(LockStyle.Layout layout) {
        boolean corner = layout == LockStyle.Layout.CORNER;
        int margin = res.getDimensionPixelSize(R.dimen.lock_corner_margin);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
        // Full width keeps the clock centred on the screen, whatever width the widgets below take on.
        params.width = corner ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT;
        params.gravity = gravityOf(layout);
        params.setMargins(corner ? margin : 0,
                layout == LockStyle.Layout.CENTERED ? res.getDimensionPixelSize(R.dimen.lock_content_top) : 0,
                0, corner ? margin : 0);
        content.setLayoutParams(params);
        int alignment = corner ? Gravity.START : Gravity.CENTER_HORIZONTAL;
        content.setGravity(alignment);
        clock.setGravity(alignment);
        widgets.setGravity(alignment);
    }

    private static int gravityOf(LockStyle.Layout layout) {
        switch (layout) {
            case CORNER:
                return Gravity.BOTTOM | Gravity.START;
            case STACKED:
                return Gravity.CENTER;
            default:
                return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        }
    }

    private static int clockSizeRes(LockStyle.Layout layout) {
        switch (layout) {
            case CORNER:
                return R.dimen.lock_clock_size_corner;
            case STACKED:
                return R.dimen.lock_clock_size_stacked;
            default:
                return R.dimen.lock_clock_size;
        }
    }
}
