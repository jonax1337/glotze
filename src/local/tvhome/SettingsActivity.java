package local.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** All device settings sections in one panel, plus the launcher's own options. */
public final class SettingsActivity extends Activity {
    private static final int[] IDLE_MINUTE_CHOICES = {5, 10, 15, 30, 60};
    private static final int[] SLEEP_MINUTE_CHOICES = {15, 30, 60, 120, 240};

    private static final RowLook SCREEN_SAVER =
            new RowLook(R.string.screen_saver, R.drawable.ic_display, R.color.tile_indigo);
    private static final RowLook IDLE_TIME =
            new RowLook(R.string.screen_saver_after, R.drawable.ic_timer, R.color.tile_teal);
    private static final RowLook SLEEP_TIME =
            new RowLook(R.string.sleep_after, R.drawable.ic_timer, R.color.tile_slate);
    private static final RowLook NIGHT =
            new RowLook(R.string.night_mode, R.drawable.ic_moon, R.color.tile_purple);
    private static final RowLook LOCK_STYLE =
            new RowLook(R.string.lock_style_title, R.drawable.ic_tune, R.color.tile_pink);
    private static final RowLook PREVIEW =
            new RowLook(R.string.screen_saver_preview, R.drawable.ic_play, R.color.tile_blue);
    private static final RowLook ORDER =
            new RowLook(R.string.app_order, R.drawable.ic_sort, R.color.tile_orange);
    private static final RowLook HIDDEN_APPS =
            new RowLook(R.string.hidden_apps_title, R.drawable.ic_hidden, R.color.tile_graphite);
    private static final RowLook SOUNDS =
            new RowLook(R.string.sounds, R.drawable.ic_sound, R.color.tile_pink);
    private static final RowLook PLACES =
            new RowLook(R.string.places_title, R.drawable.ic_place, R.color.tile_green);
    private static final RowLook BACKUP =
            new RowLook(R.string.settings_backup_title, R.drawable.ic_backup, R.color.tile_sky);
    private static final RowLook APP_LOCK =
            new RowLook(R.string.settings_applock_title, R.drawable.ic_shield, R.color.tile_violet);

    private UiSounds sounds;
    private TileArrangement arrangement;
    private AppCatalog catalog;
    private ScreenSaver screenSaver;
    private NightMode nightMode;
    private LockStyle lockStyle;
    private ActivityStarter starter;
    private OptionsSheet sheet;
    private AppLock appLock;
    private PinDialog pinDialog;
    private FocusScrollView scroller;
    private LinearLayout rows;
    private GlotzeView glotze;
    private TextView screenSaverState;
    private TextView idleTime;
    private TextView sleepTime;
    private TextView nightState;
    private TextView orderState;
    private TextView hiddenCount;
    private TextView soundState;
    private TextView appLockState;
    private boolean backupRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Fonts fonts = new Fonts(getAssets());
        sounds = new UiSounds(this);
        arrangement = new TileArrangement(this);
        catalog = new AppCatalog(this);
        screenSaver = new ScreenSaver(this);
        nightMode = new NightMode(this);
        lockStyle = new LockStyle(this);
        starter = new ActivityStarter(this);
        sheet = new OptionsSheet(this, sounds, fonts);
        appLock = new AppLock(this);
        pinDialog = new PinDialog(this, appLock, sounds, fonts);
        scroller = findViewById(R.id.settings_scroller);
        rows = findViewById(R.id.settings_rows);
        glotze = findViewById(R.id.settings_glotze);
        dressInSoftSolid();
        showDeviceSummary();
        showVersion();
        addHeading(R.string.section_device);
        addDeviceSections();
        addHeading(R.string.section_screen_saver);
        screenSaverState = addRow(SCREEN_SAVER, this::toggleScreenSaver);
        idleTime = addRow(IDLE_TIME, this::chooseIdleTime);
        sleepTime = addRow(SLEEP_TIME, this::chooseSleepTime);
        addRow(LOCK_STYLE, () -> starter.start(new Intent(this, LockStyleActivity.class),
                getString(R.string.lock_style_title)));
        addRow(PREVIEW, this::previewScreenSaver);
        addHeading(R.string.section_home);
        orderState = addRow(ORDER, this::toggleOrdering);
        hiddenCount = addRow(HIDDEN_APPS, this::showHiddenApps);
        soundState = addRow(SOUNDS, this::toggleSounds);
        nightState = addRow(NIGHT, this::toggleNightMode);
        addHeading(R.string.settings_section_glotze);
        addRow(PLACES, () -> starter.start(new Intent(this, PlacesActivity.class), getString(R.string.places_title)));
        addRow(BACKUP, this::chooseBackupAction);
        appLockState = addRow(APP_LOCK, this::chooseAppLockAction);
        fonts.applyTo(findViewById(android.R.id.content));
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(this::onFocusMoved);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showValues();
        glotze.setAsleep(nightMode.isActive());
        glotze.loop(GlotzeView.Motion.IDLE);
    }

    @Override
    protected void onPause() {
        glotze.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        sounds.release();
        super.onDestroy();
    }

    private void dressInSoftSolid() {
        Resources res = getResources();
        // The root paints the ground colour; skipping the window background saves another full-screen fill.
        getWindow().setBackgroundDrawable(null);
        findViewById(R.id.settings_badge).setBackground(
                SurfaceDrawable.card(res, res.getDimension(R.dimen.surface_card_corner)));
    }

    /** "Living room TV · Android 7.1.2", from the name the user gave the device. */
    private void showDeviceSummary() {
        String deviceName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
        String name = deviceName != null ? deviceName : Build.MODEL;
        TextView summary = findViewById(R.id.settings_device);
        summary.setText(getString(R.string.device_summary, name, Build.VERSION.RELEASE));
    }

    private void showVersion() {
        TextView version = findViewById(R.id.settings_version);
        version.setText(getString(R.string.settings_version, installedVersionName()));
    }

    private String installedVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException notInstalled) {
            throw new IllegalStateException("The running app is not installed", notInstalled);
        }
    }

    private void addDeviceSections() {
        for (SystemSettings.Section section : SystemSettings.available(this)) {
            addRow(section.look, () -> starter.start(section.intent, getString(section.look.labelRes)));
        }
    }

    private void addHeading(int textRes) {
        TextView heading = (TextView) getLayoutInflater().inflate(R.layout.settings_heading, rows, false);
        heading.setText(textRes);
        rows.addView(heading);
    }

    /** Adds a row and returns its value field for later updates. */
    private TextView addRow(RowLook look, Runnable action) {
        Resources res = getResources();
        View row = getLayoutInflater().inflate(R.layout.settings_row, rows, false);
        row.setBackground(SurfaceDrawable.focusableCard(res, res.getDimension(R.dimen.surface_tile_corner)));
        ImageView icon = row.findViewById(R.id.row_icon);
        TextView label = row.findViewById(R.id.row_label);
        icon.setImageResource(look.iconRes);
        icon.setBackground(iconTile(look.colorRes));
        label.setText(look.labelRes);
        row.setOnClickListener(view -> {
            sounds.playSelect();
            action.run();
        });
        rows.addView(row);
        return row.findViewById(R.id.row_value);
    }

    private GradientDrawable iconTile(int colorRes) {
        GradientDrawable tile = new GradientDrawable();
        tile.setColor(getColor(colorRes));
        tile.setCornerRadius(getResources().getDimension(R.dimen.icon_tile_corner));
        return tile;
    }

    private void onFocusMoved(View oldFocus, View newFocus) {
        if (newFocus != null && rows.indexOfChild(newFocus) >= 0) {
            scroller.glideToReveal(newFocus);
        }
        if (oldFocus != null) {
            sounds.playFocus();
        }
    }

    private void showValues() {
        screenSaverState.setText(!screenSaver.canChoose() ? R.string.needs_adb_grant
                : screenSaver.isOwn() ? R.string.screen_saver_own : R.string.screen_saver_fire_tv);
        idleTime.setText(getString(R.string.minutes, screenSaver.idleMinutes()));
        sleepTime.setText(screenSaver.sleepsWhenIdle()
                ? getString(R.string.minutes, screenSaver.sleepMinutes()) : getString(R.string.never));
        nightState.setText(nightMode.isEnabled() ? nightScheduleText() : getString(R.string.off));
        orderState.setText(arrangement.isOrderedByUsage() ? R.string.app_order_usage : R.string.app_order_fixed);
        hiddenCount.setText(String.valueOf(arrangement.hiddenAlphabetical(catalog.loadApps()).size()));
        soundState.setText(sounds.areEnabled() ? R.string.on : R.string.off);
        appLockState.setText(appLock.hasPin() ? R.string.on : R.string.off);
    }

    private String nightScheduleText() {
        return lockStyle.nightFollowsSun()
                ? getString(R.string.night_mode_sun)
                : getString(R.string.night_mode_hours, lockStyle.nightStartHour(), lockStyle.nightEndHour());
    }

    private void toggleScreenSaver() {
        if (!screenSaver.canChoose()) {
            Toast.makeText(this, R.string.screen_saver_grant_hint, Toast.LENGTH_LONG).show();
            return;
        }
        screenSaver.toggle();
        showValues();
    }

    private void chooseIdleTime() {
        if (!screenSaver.canChangeIdleTime()) {
            Toast.makeText(this, R.string.idle_time_grant_hint, Toast.LENGTH_LONG).show();
            return;
        }
        chooseMinutes(R.string.screen_saver_after, IDLE_MINUTE_CHOICES, screenSaver::setIdleMinutes);
    }

    private void chooseSleepTime() {
        if (!screenSaver.canChoose()) {
            Toast.makeText(this, R.string.screen_saver_grant_hint, Toast.LENGTH_LONG).show();
            return;
        }
        chooseMinutes(R.string.sleep_after, SLEEP_MINUTE_CHOICES, screenSaver::setSleepMinutes);
    }

    private void chooseMinutes(int titleRes, int[] choices, IntConsumer apply) {
        List<OptionsSheet.Option> options = new ArrayList<>();
        for (int minutes : choices) {
            options.add(new OptionsSheet.Option(getString(R.string.minutes, minutes), () -> {
                apply.accept(minutes);
                showValues();
            }));
        }
        sheet.show(getString(titleRes), options);
    }

    private void toggleNightMode() {
        nightMode.toggle();
        showValues();
    }

    private void previewScreenSaver() {
        starter.start(new Intent(this, LockScreenActivity.class), getString(R.string.screen_saver));
    }

    private void toggleOrdering() {
        arrangement.toggleOrdering();
        showValues();
    }

    private void showHiddenApps() {
        List<AppTile> hidden = arrangement.hiddenAlphabetical(catalog.loadApps());
        if (hidden.isEmpty()) {
            Toast.makeText(this, R.string.no_hidden_apps, Toast.LENGTH_SHORT).show();
            return;
        }
        List<OptionsSheet.Option> options = new ArrayList<>();
        for (AppTile app : hidden) {
            options.add(new OptionsSheet.Option(app.label, () -> unhide(app)));
        }
        sheet.show(getString(R.string.unhide_title), options);
    }

    private void unhide(AppTile app) {
        arrangement.unhide(app.key);
        showValues();
    }

    private void toggleSounds() {
        sounds.toggle();
        showValues();
    }

    private void chooseBackupAction() {
        List<OptionsSheet.Option> options = new ArrayList<>();
        options.add(new OptionsSheet.Option(getString(R.string.settings_backup_export), this::exportBackup));
        options.add(new OptionsSheet.Option(getString(R.string.settings_backup_import), this::confirmImport));
        sheet.show(getString(R.string.settings_backup_title), options);
    }

    private void exportBackup() {
        runBackup(() -> BackupManager.export(this), R.string.settings_backup_exported, () -> { });
    }

    private void confirmImport() {
        File file = BackupManager.backupFile(this);
        String path = file != null ? file.getAbsolutePath() : "";
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.settings_backup_import)
                .setMessage(getString(R.string.settings_backup_confirm, path))
                .setPositiveButton(R.string.settings_ok, (dialog, which) -> importBackup())
                .setNegativeButton(R.string.settings_cancel, null)
                .show();
    }

    private void importBackup() {
        // recreate() drops the preference values this screen has cached (e.g. the tile arrangement).
        runBackup(() -> BackupManager.restore(this), R.string.settings_backup_imported, this::recreate);
    }

    /** Runs the file work off the UI thread and reports the outcome in a dialog that stays until dismissed. */
    private void runBackup(Supplier<BackupManager.Result> task, int successRes, Runnable afterNotice) {
        if (backupRunning) {
            return;
        }
        backupRunning = true;
        new Thread(() -> {
            BackupManager.Result result = task.get();
            runOnUiThread(() -> showBackupResult(result, successRes, afterNotice));
        }).start();
    }

    private void showBackupResult(BackupManager.Result result, int successRes, Runnable afterNotice) {
        backupRunning = false;
        if (isDestroyed()) {
            return;
        }
        if (!result.success) {
            showNotice(R.string.settings_backup_title,
                    getString(R.string.settings_backup_failed, result.errorMessage), () -> { });
            return;
        }
        String path = BackupManager.backupFile(this).getAbsolutePath();
        showNotice(R.string.settings_backup_title,
                getString(successRes, result.entryCount, path), afterNotice);
    }

    private void showNotice(int titleRes, String message, Runnable afterDismiss) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(titleRes)
                .setMessage(message)
                .setPositiveButton(R.string.settings_ok, null)
                .setOnDismissListener(dialog -> afterDismiss.run())
                .show();
    }

    private void chooseAppLockAction() {
        if (!appLock.hasPin()) {
            pinDialog.showSet(afterConfirm(this::explainAppLockScope));
            return;
        }
        List<OptionsSheet.Option> options = new ArrayList<>();
        options.add(new OptionsSheet.Option(getString(R.string.settings_applock_change),
                () -> pinDialog.showVerify(afterConfirm(() -> pinDialog.showSet(afterConfirm(this::showValues))))));
        options.add(new OptionsSheet.Option(getString(R.string.settings_applock_remove),
                () -> pinDialog.showVerify(afterConfirm(this::removePin))));
        options.add(new OptionsSheet.Option(getString(R.string.settings_applock_note), this::explainAppLockScope));
        sheet.show(getString(R.string.settings_applock_title), options);
    }

    private void removePin() {
        appLock.clearPin();
        showValues();
    }

    private void explainAppLockScope() {
        showValues();
        showNotice(R.string.settings_applock_title, getString(R.string.settings_applock_note_text), () -> { });
    }

    private PinDialog.Callback afterConfirm(Runnable next) {
        return new PinDialog.Callback() {
            @Override
            public void onConfirmed() {
                next.run();
            }

            @Override
            public void onCancelled() {
            }
        };
    }
}
