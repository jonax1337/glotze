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
 * Like SF Text and SF Display on Apple's systems, large text takes the design's display cut, drawn
 * tighter and finer for its size: InterDisplay in Glas, Figtree itself in Soft Solid.
 */
final class Fonts {
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_MEDIUM = "medium";
    private static final String TAG_SEMIBOLD = "semibold";
    private static final String TAG_BOLD = "bold";

    private final Weights text;
    private final Weights display;
    private final float displayMinSize;

    Fonts(Context context) {
        AssetManager assets = context.getAssets();
        text = new Weights(assets, ThemeValues.string(context, R.attr.uiFontFamily));
        display = new Weights(assets, ThemeValues.string(context, R.attr.displayFontFamily));
        displayMinSize = context.getResources().getDimension(R.dimen.display_font_min_size);
    }

    void applyTo(View root) {
        if (root instanceof TextView) {
            TextView view = (TextView) root;
            view.setTypeface(cutFor(view).typefaceFor(view.getTag()));
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTo(group.getChildAt(i));
            }
        }
    }

    private Weights cutFor(TextView view) {
        return view.getTextSize() >= displayMinSize ? display : text;
    }

    /** The five weights of one cut, loaded from assets/fonts/<family>-<weight>.ttf. */
    private static final class Weights {
        private final Typeface light;
        private final Typeface regular;
        private final Typeface medium;
        private final Typeface semibold;
        private final Typeface bold;

        Weights(AssetManager assets, String family) {
            light = load(assets, family, "Light");
            regular = load(assets, family, "Regular");
            medium = load(assets, family, "Medium");
            semibold = load(assets, family, "SemiBold");
            bold = load(assets, family, "Bold");
        }

        private static Typeface load(AssetManager assets, String family, String weight) {
            return Typeface.createFromAsset(assets, "fonts/" + family + "-" + weight + ".ttf");
        }

        Typeface typefaceFor(Object tag) {
            if (TAG_DISPLAY.equals(tag)) {
                return light;
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
}
