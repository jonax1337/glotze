package local.tvhome;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.util.List;

/**
 * Styles the screen saver like the iOS lock screen editor: the live screen saver, and a flat bar
 * where ▲▼ pick a setting and ◀▶ its variant. Every change is saved and shown at once.
 */
public final class LockStyleActivity extends Activity {
    private static final long BAR_VISIBLE_MS = 3000;
    private static final long BAR_FADE_MS = 400;
    private static final int CALENDAR_ACCESS_REQUEST = 1;

    private final Runnable hideBar = this::hideBar;
    private View bar;
    private LockStyle style;
    private LockScreen lockScreen;
    private UiSounds sounds;
    private TextView settingLabel;
    private TextView choiceLabel;
    private int selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Design.dress(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_style_editor);
        style = new LockStyle(this);
        sounds = new UiSounds(this);
        View root = findViewById(android.R.id.content);
        lockScreen = new LockScreen(root, this);
        findViewById(R.id.lock_hint).setVisibility(View.GONE);
        settingLabel = findViewById(R.id.lock_editor_setting);
        choiceLabel = findViewById(R.id.lock_editor_choice);
        Resources res = getResources();
        bar = findViewById(R.id.lock_editor_bar);
        bar.setBackground(SurfaceDrawable.raised(this, res.getDimension(R.dimen.surface_card_corner)));
        showSelection();
        revealBar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        lockScreen.start();
    }

    @Override
    protected void onStop() {
        lockScreen.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lockScreen.release();
        sounds.release();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }
        revealBar();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
                select(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                select(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                change(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                change(1);
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    /** The bar steps aside after a moment, so the whole screen saver can be judged. */
    private void revealBar() {
        bar.removeCallbacks(hideBar);
        bar.animate().alpha(1f).setDuration(BAR_FADE_MS).start();
        bar.postDelayed(hideBar, BAR_VISIBLE_MS);
    }

    private void hideBar() {
        bar.animate().alpha(0f).setDuration(BAR_FADE_MS).start();
    }

    private void select(int delta) {
        selected = Math.floorMod(selected + delta, style.adjustable().size());
        sounds.playFocus();
        showSelection();
    }

    private void change(int delta) {
        LockStyle.Setting setting = selectedSetting();
        style.step(setting, delta);
        lockScreen.applyStyle();
        requestCalendarAccessIfChosenIn(setting);
        sounds.playFocus();
        showSelection();
    }

    /** The calendar widget shows nothing without the permission, so it is asked for as soon as the widget is chosen. */
    private void requestCalendarAccessIfChosenIn(LockStyle.Setting setting) {
        if (style.holds(setting, LockStyle.Widget.CALENDAR) && !CalendarWidget.hasAccess(this)) {
            requestPermissions(new String[] {CalendarWidget.PERMISSION}, CALENDAR_ACCESS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALENDAR_ACCESS_REQUEST) {
            lockScreen.applyStyle();
        }
    }

    /** Changing the font can remove the width setting, so the selection is kept within the list. */
    private LockStyle.Setting selectedSetting() {
        List<LockStyle.Setting> adjustable = style.adjustable();
        selected = Math.min(selected, adjustable.size() - 1);
        return adjustable.get(selected);
    }

    private void showSelection() {
        LockStyle.Setting setting = selectedSetting();
        settingLabel.setText(setting.labelRes);
        choiceLabel.setText(getString(R.string.lock_style_choice, style.choiceLabel(setting)));
    }
}
