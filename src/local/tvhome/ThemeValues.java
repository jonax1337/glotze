package local.tvhome;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;

/** Reads what the current design sets in the theme, see res/values/design_attrs.xml. */
final class ThemeValues {
    private ThemeValues() {
    }

    static int color(Context context, int attr) {
        return colors(context, attr).getDefaultColor();
    }

    /** The colours by state, e.g. the dark text of a focused row in Glas. */
    static ColorStateList colors(Context context, int attr) {
        TypedArray values = context.obtainStyledAttributes(new int[] {attr});
        try {
            ColorStateList colors = values.getColorStateList(0);
            if (colors == null) {
                throw missing(context, attr);
            }
            return colors;
        } finally {
            values.recycle();
        }
    }

    static float dimension(Context context, int attr) {
        TypedArray values = context.obtainStyledAttributes(new int[] {attr});
        try {
            if (!values.hasValue(0)) {
                throw missing(context, attr);
            }
            return values.getDimension(0, 0f);
        } finally {
            values.recycle();
        }
    }

    /** A resource the design names, e.g. its version of an icon. */
    static int resource(Context context, int attr) {
        TypedArray values = context.obtainStyledAttributes(new int[] {attr});
        try {
            int id = values.getResourceId(0, 0);
            if (id == 0) {
                throw missing(context, attr);
            }
            return id;
        } finally {
            values.recycle();
        }
    }

    static String string(Context context, int attr) {
        TypedArray values = context.obtainStyledAttributes(new int[] {attr});
        try {
            String text = values.getString(0);
            if (text == null) {
                throw missing(context, attr);
            }
            return text;
        } finally {
            values.recycle();
        }
    }

    private static IllegalStateException missing(Context context, int attr) {
        return new IllegalStateException("The theme sets no " + context.getResources().getResourceEntryName(attr));
    }
}
