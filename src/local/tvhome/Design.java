package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ContextThemeWrapper;

/** The two looks of the launcher. The settings choose one; every screen dresses in it before it inflates. */
enum Design {
    SOFT_SOLID(R.style.HomeTheme, R.string.design_soft_solid),
    GLASS(R.style.HomeTheme_Glass, R.string.design_glass);

    private static final String PREFERENCES = "ui";
    private static final String KEY_DESIGN = "design";

    final int labelRes;
    private final int themeRes;

    Design(int themeRes, int labelRes) {
        this.themeRes = themeRes;
        this.labelRes = labelRes;
    }

    static Design current(Context context) {
        String stored = preferences(context).getString(KEY_DESIGN, SOFT_SOLID.name());
        for (Design design : values()) {
            if (design.name().equals(stored)) {
                return design;
            }
        }
        return SOFT_SOLID;
    }

    static void choose(Context context, Design design) {
        preferences(context).edit().putString(KEY_DESIGN, design.name()).apply();
    }

    /** Applies the chosen design's theme and returns it; must run before the first view is inflated. */
    static Design dress(Context context) {
        Design design = current(context);
        context.setTheme(design.themeRes);
        return design;
    }

    /** For a window the system creates, like the screen saver's, whose own theme a service cannot set. */
    static Context dressed(Context base) {
        return new ContextThemeWrapper(base, current(base).themeRes);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
