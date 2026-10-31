package local.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** A raised panel that glides in from the right edge and offers a list of choices. */
final class OptionsSheet {
    static final class Option {
        final CharSequence label;
        final Runnable action;

        Option(CharSequence label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final UiSounds sounds;
    private final Fonts fonts;

    OptionsSheet(Context context, UiSounds sounds, Fonts fonts) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.sounds = sounds;
        this.fonts = fonts;
    }

    void show(CharSequence title, List<Option> options) {
        Dialog dialog = new Dialog(context, R.style.SheetDialog);
        View sheet = inflater.inflate(R.layout.sheet, null);
        TextView heading = sheet.findViewById(R.id.sheet_title);
        LinearLayout rows = sheet.findViewById(R.id.sheet_rows);
        Resources res = context.getResources();
        sheet.findViewById(R.id.sheet_panel).setBackground(SurfaceDrawable.raised(context, res.getDimension(R.dimen.surface_card_corner)));
        heading.setText(title);
        for (Option option : options) {
            rows.addView(newRow(rows, dialog, option));
        }
        FocusScrollView scroller = sheet.findViewById(R.id.sheet_scroller);
        sheet.getViewTreeObserver().addOnGlobalFocusChangeListener(
                (oldFocus, newFocus) -> followFocus(scroller, oldFocus, newFocus));
        fonts.applyTo(sheet);
        dialog.setContentView(sheet);
        pinToRightEdge(dialog.getWindow());
        dialog.show();
    }

    private View newRow(ViewGroup rows, Dialog dialog, Option option) {
        TextView row = (TextView) inflater.inflate(R.layout.sheet_row, rows, false);
        Resources res = context.getResources();
        row.setBackground(SurfaceDrawable.focusableCard(context, res.getDimension(R.dimen.surface_tile_corner)));
        row.setText(option.label);
        row.setOnClickListener(view -> {
            sounds.playSelect();
            dialog.dismiss();
            option.action.run();
        });
        return row;
    }

    /** Long lists scroll with the focus; every move after the first plays the focus sound. */
    private void followFocus(FocusScrollView scroller, View oldFocus, View newFocus) {
        if (newFocus != null) {
            scroller.glideToReveal(newFocus);
        }
        if (oldFocus != null) {
            sounds.playFocus();
        }
    }

    private void pinToRightEdge(Window window) {
        int width = context.getResources().getDimensionPixelSize(R.dimen.sheet_width);
        window.setGravity(Gravity.END);
        window.setLayout(width, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
