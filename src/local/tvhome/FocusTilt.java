package local.tvhome;

import android.content.Context;
import android.view.View;

/**
 * How a focused tile leans in Glas, the way the tiles of tvOS turn under the light: the edge facing
 * the move recedes, as if the tile were pushed that way. Soft Solid sets no lean, so its tiles stay flat.
 */
final class FocusTilt {
    private final float degrees;

    FocusTilt(Context context) {
        degrees = ThemeValues.number(context, R.attr.focusTiltDegrees);
    }

    /** The turn around the vertical axis for a move in this {@code View.FOCUS_*} direction. */
    float rotationY(int direction) {
        if (direction == View.FOCUS_RIGHT) {
            return degrees;
        }
        return direction == View.FOCUS_LEFT ? -degrees : 0f;
    }

    /** The turn around the horizontal axis for a move in this {@code View.FOCUS_*} direction. */
    float rotationX(int direction) {
        if (direction == View.FOCUS_UP) {
            return degrees;
        }
        return direction == View.FOCUS_DOWN ? -degrees : 0f;
    }
}
