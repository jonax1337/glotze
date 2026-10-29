package local.tvhome;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/** A frame whose height always follows its width at 16:9, the TV banner format. */
public final class AspectFrameLayout extends FrameLayout {
    private static final float HEIGHT_PER_WIDTH = 9f / 16f;

    public AspectFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width * HEIGHT_PER_WIDTH);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
