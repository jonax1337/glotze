package local.tvhome;

import android.animation.AnimatorInflater;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A flat number pad for the app lock PIN, operable with the D-pad alone. It checks or stores
 * the PIN through {@link AppLock} itself, so the PIN never leaves this class and is neither
 * logged nor handed on in an intent. Back cancels.
 */
final class PinDialog {
    interface Callback {
        /** The PIN was verified, or the new PIN has been stored. */
        void onConfirmed();

        void onCancelled();
    }

    private enum Mode { VERIFY, SET }

    /** Text weights are picked by tag, see {@link Fonts}. */
    private static final String SEMIBOLD_TAG = "semibold";
    private static final int DIGITS_PER_ROW = 3;
    private static final int DIGIT_ROWS = 3;
    private static final int MILLIS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;

    private final Context context;
    private final Resources res;
    private final AppLock lock;
    private final UiSounds sounds;
    private final Fonts fonts;

    PinDialog(Context context, AppLock lock, UiSounds sounds, Fonts fonts) {
        this.context = context;
        this.res = context.getResources();
        this.lock = lock;
        this.sounds = sounds;
        this.fonts = fonts;
    }

    /** Asks for the current PIN; confirms only when it is right. */
    void showVerify(Callback callback) {
        new Session(Mode.VERIFY, callback).show();
    }

    /** Asks for a new PIN twice and stores it once both entries agree. */
    void showSet(Callback callback) {
        new Session(Mode.SET, callback).show();
    }

    /** One appearance of the dialog, with the digits typed so far. */
    private final class Session {
        private final Mode mode;
        private final Callback callback;
        private final Dialog dialog = new Dialog(context, R.style.SheetDialog);
        private final StringBuilder entry = new StringBuilder();
        private final TextView title = newText(R.color.text_primary, R.dimen.pin_title_text);
        private final TextView message = newText(R.color.text_muted, R.dimen.pin_message_text);
        private final LinearLayout dots = new LinearLayout(context);
        /** The first entry while a new PIN waits for its repetition, otherwise empty. */
        private String firstEntry = "";

        Session(Mode mode, Callback callback) {
            this.mode = mode;
            this.callback = callback;
            dialog.setContentView(buildContent());
            dialog.setOnCancelListener(cancelled -> {
                wipe();
                callback.onCancelled();
            });
            centerOnScreen(dialog.getWindow());
            showPrompt();
            refreshDots();
        }

        void show() {
            dialog.show();
        }

        private View buildContent() {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setClipChildren(false);
            root.setClipToPadding(false);
            int padding = res.getDimensionPixelSize(R.dimen.pin_dialog_padding);
            root.setPadding(padding, padding, padding, padding);
            root.setBackground(SurfaceDrawable.raised(res, res.getDimension(R.dimen.surface_card_corner)));

            int gap = res.getDimensionPixelSize(R.dimen.pin_section_gap);
            title.setTag(SEMIBOLD_TAG);
            message.setGravity(Gravity.CENTER);
            message.setMinLines(2);
            dots.setGravity(Gravity.CENTER);
            dots.setClipChildren(false);
            addBelow(root, title, gap);
            addBelow(root, message, gap);
            addBelow(root, dots, gap);
            addBelow(root, buildKeypad(), 0);

            root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
                if (oldFocus != null) {
                    sounds.playFocus();
                }
            });
            fonts.applyTo(root);
            return root;
        }

        private void addBelow(LinearLayout root, View view, int bottomMargin) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = bottomMargin;
            root.addView(view, params);
        }

        private View buildKeypad() {
            LinearLayout pad = new LinearLayout(context);
            pad.setOrientation(LinearLayout.VERTICAL);
            pad.setClipChildren(false);
            for (int row = 0; row < DIGIT_ROWS; row++) {
                LinearLayout line = newKeyRow();
                for (int column = 0; column < DIGITS_PER_ROW; column++) {
                    line.addView(digitKey(row * DIGITS_PER_ROW + column + 1));
                }
                pad.addView(line);
            }
            LinearLayout bottom = newKeyRow();
            bottom.addView(newKey(res.getString(R.string.pin_key_delete), this::deleteDigit));
            bottom.addView(digitKey(0));
            bottom.addView(newKey(res.getString(R.string.pin_key_ok), this::submit));
            pad.addView(bottom);
            return pad;
        }

        private LinearLayout newKeyRow() {
            LinearLayout line = new LinearLayout(context);
            line.setGravity(Gravity.CENTER);
            line.setClipChildren(false);
            return line;
        }

        private View digitKey(int digit) {
            return newKey(Integer.toString(digit), () -> addDigit(digit));
        }

        private View newKey(CharSequence label, Runnable action) {
            TextView key = new TextView(context);
            key.setText(label);
            key.setTag(SEMIBOLD_TAG);
            key.setGravity(Gravity.CENTER);
            key.setTextColor(res.getColor(R.color.text_primary, null));
            key.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.pin_key_text));
            key.setFocusable(true);
            key.setBackground(keyBackground());
            key.setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, R.animator.row_lift));
            key.setOnClickListener(view -> {
                sounds.playSelect();
                action.run();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    res.getDimensionPixelSize(R.dimen.pin_key_width),
                    res.getDimensionPixelSize(R.dimen.pin_key_height));
            int gapHalf = res.getDimensionPixelSize(R.dimen.pin_key_gap_half);
            params.setMargins(gapHalf, gapHalf, gapHalf, gapHalf);
            key.setLayoutParams(params);
            return key;
        }

        private Drawable keyBackground() {
            return SurfaceDrawable.focusableCard(res, res.getDimension(R.dimen.surface_tile_corner));
        }

        private void addDigit(int digit) {
            if (entry.length() < AppLock.MAX_PIN_LENGTH) {
                entry.append(digit);
                showPrompt();
                refreshDots();
            }
        }

        private void deleteDigit() {
            if (entry.length() > 0) {
                entry.setLength(entry.length() - 1);
                refreshDots();
            }
        }

        private void submit() {
            if (entry.length() < AppLock.MIN_PIN_LENGTH) {
                showError(res.getString(R.string.pin_too_short, AppLock.MIN_PIN_LENGTH));
            } else if (mode == Mode.VERIFY) {
                submitVerify();
            } else {
                submitSet();
            }
        }

        private void submitVerify() {
            AppLock.Verdict verdict = lock.verify(entry.toString());
            if (verdict.state == AppLock.State.OK) {
                confirm();
            } else if (verdict.state == AppLock.State.LOCKED_UNTIL) {
                rejectEntry(lockedMessage(verdict.lockedUntilMillis));
            } else {
                rejectEntry(res.getString(R.string.pin_wrong));
            }
        }

        private void submitSet() {
            if (!isAwaitingRepetition()) {
                firstEntry = entry.toString();
                entry.setLength(0);
                showPrompt();
                refreshDots();
            } else if (firstEntry.contentEquals(entry)) {
                lock.setPin(firstEntry);
                confirm();
            } else {
                firstEntry = "";
                title.setText(promptTitle());
                rejectEntry(res.getString(R.string.pin_mismatch));
            }
        }

        private boolean isAwaitingRepetition() {
            return !firstEntry.isEmpty();
        }

        private void confirm() {
            wipe();
            dialog.dismiss();
            callback.onConfirmed();
        }

        private void wipe() {
            clearEntry();
            firstEntry = "";
        }

        /** Overwrites the digits first, setLength alone would leave them in the buffer. */
        private void clearEntry() {
            for (int i = 0; i < entry.length(); i++) {
                entry.setCharAt(i, '0');
            }
            entry.setLength(0);
        }

        private void rejectEntry(String reason) {
            clearEntry();
            refreshDots();
            showError(reason);
        }

        private String lockedMessage(long lockedUntilMillis) {
            long remainingMillis = lockedUntilMillis - System.currentTimeMillis();
            long seconds = Math.max(1, (remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND);
            if (seconds < SECONDS_PER_MINUTE) {
                return res.getString(R.string.pin_locked_seconds, (int) seconds);
            }
            long minutes = (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE;
            return res.getString(R.string.pin_locked_minutes, (int) minutes);
        }

        private void showPrompt() {
            title.setText(promptTitle());
            showMessage(mode == Mode.SET ? lengthHint() : "", R.color.text_muted);
        }

        private int promptTitle() {
            if (mode == Mode.VERIFY) {
                return R.string.pin_title_verify;
            }
            return isAwaitingRepetition() ? R.string.pin_title_repeat : R.string.pin_title_new;
        }

        private String lengthHint() {
            return res.getString(R.string.pin_hint_length, AppLock.MIN_PIN_LENGTH, AppLock.MAX_PIN_LENGTH);
        }

        private void showError(String text) {
            showMessage(text, R.color.tile_red);
        }

        private void showMessage(String text, int colorResource) {
            message.setTextColor(res.getColor(colorResource, null));
            message.setText(text);
        }

        /** One dot per typed digit, with empty placeholders up to the shortest allowed PIN. */
        private void refreshDots() {
            dots.removeAllViews();
            int slots = Math.max(entry.length(), AppLock.MIN_PIN_LENGTH);
            for (int slot = 0; slot < slots; slot++) {
                dots.addView(newDot(slot < entry.length()));
            }
        }

        private View newDot(boolean filled) {
            int color = res.getColor(filled ? R.color.text_primary : R.color.text_muted, null);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            if (filled) {
                shape.setColor(color);
            } else {
                shape.setStroke(res.getDimensionPixelSize(R.dimen.pin_dot_stroke), color);
            }
            View dot = new View(context);
            dot.setBackground(shape);
            int size = res.getDimensionPixelSize(R.dimen.pin_dot_size);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            int gapHalf = res.getDimensionPixelSize(R.dimen.pin_dot_gap_half);
            params.setMargins(gapHalf, 0, gapHalf, 0);
            dot.setLayoutParams(params);
            return dot;
        }
    }

    private TextView newText(int colorResource, int sizeResource) {
        TextView text = new TextView(context);
        text.setTextColor(res.getColor(colorResource, null));
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(sizeResource));
        return text;
    }

    private void centerOnScreen(Window window) {
        window.setWindowAnimations(android.R.style.Animation_Dialog);
        window.setGravity(Gravity.CENTER);
        window.setLayout(res.getDimensionPixelSize(R.dimen.pin_dialog_width), ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
