package local.tvhome;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * The type family of the current design, Figtree for Soft Solid and Inter for Glas (both SIL Open
 * Font License, see assets/fonts). Android 7 has no font resources, so text views pick their weight
 * through android:tag: "display" (Light), "medium", "semibold", "bold", or regular when untagged.
 */
final class Fonts {
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_MEDIUM = "medium";
    private static final String TAG_SEMIBOLD = "semibold";
    private static final String TAG_BOLD = "bold";

    private final Typeface display;
    private final Typeface regular;
    private final Typeface medium;
    private final Typeface semibold;
    private final Typeface bold;

    Fonts(Context context) {
        AssetManager assets = context.getAssets();
        String family = ThemeValues.string(context, R.attr.uiFontFamily);
        display = load(assets, family, "Light");
        regular = load(assets, family, "Regular");
        medium = load(assets, family, "Medium");
        semibold = load(assets, family, "SemiBold");
        bold = load(assets, family, "Bold");
    }

    void applyTo(View root) {
        if (root instanceof TextView) {
            ((TextView) root).setTypeface(typefaceFor(root.getTag()));
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTo(group.getChildAt(i));
            }
        }
    }

    private static Typeface load(AssetManager assets, String family, String weight) {
        return Typeface.createFromAsset(assets, "fonts/" + family + "-" + weight + ".ttf");
    }

    private Typeface typefaceFor(Object tag) {
        if (TAG_DISPLAY.equals(tag)) {
            return display;
        }
        if (TAG_MEDIUM.equals(tag)) {
            return medium;
        }
        if (TAG_SEMIBOLD.equals(tag)) {
            return semibold;
        }
        return TAG_BOLD.equals(tag) ? bold : regular;
    }
}
