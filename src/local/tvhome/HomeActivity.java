package local.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class HomeActivity extends Activity
        implements TileGrid.Listener, TileMenu.Actions {
    private static final long MINUTE_MS = 60_000;
    private static final long NIGHT_FADE_MS = 2000;

    private AppCatalog catalog;
    private TileArrangement arrangement;
    private UiSounds sounds;
    private ActivityStarter starter;
    private HomeShelf shelf;
    private TileGrid grid;
    private HomeNavigation navigation;
    private TileMenu menu;
    private TileMover mover;
    private AppLock appLock;
    private PinDialog pinDialog;
    private NetworkIndicator network;
    private WeatherService weather;
    private WeatherChip weatherChip;
    private DepartureChip departureChip;
    private FocusScrollView scroller;
    private View hero;
    private LinearLayout tileRows;
    private ScreenSaver screenSaver;
    private NightMode nightMode;
    private View nightShade;
    private HomeHints hints;
    private GlotzeView glotze;
    /** Glas has no Glotze on the stage. */
    private boolean glotzeOnStage;
    private Design design;
    private final Handler idleTimer = new Handler(Looper.getMainLooper());
    private final Runnable lockWhenIdle = this::showLockScreen;
    private final Runnable followNight = this::followNight;
    private final int[] screenPosition = new int[2];
    private boolean focusFirstTileOnResume;
    private boolean resumed;
    private boolean pinPromptOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        design = Design.dress(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Fonts fonts = new Fonts(this);
        fonts.applyTo(findViewById(android.R.id.content));
        catalog = new AppCatalog(this);
        arrangement = new TileArrangement(this);
        sounds = new UiSounds(this);
        starter = new ActivityStarter(this);
        appLock = new AppLock(this);
        pinDialog = new PinDialog(this, appLock, sounds, fonts);
        scroller = findViewById(R.id.scroller);
        scroller.setTopClearance(getResources().getDimensionPixelSize(R.dimen.stage_grid_top));
        tileRows = findViewById(R.id.tile_rows);
        OptionsSheet sheet = new OptionsSheet(this, sounds, fonts);
        shelf = new HomeShelf(findViewById(android.R.id.content), this);
        grid = new TileGrid(scroller, fonts, this);
        navigation = new HomeNavigation(shelf, grid, findViewById(R.id.status_pill), findViewById(R.id.settings_button));
        menu = new TileMenu(this, sheet, starter, this);
        mover = new TileMover(grid, this::saveMovedTiles);
        // The root view paints the ground; skipping the window background saves a full-screen fill per frame.
        getWindow().setBackgroundDrawable(null);
        hints = new HomeHints(findViewById(R.id.home_hint), starter);
        network = new NetworkIndicator(findViewById(R.id.network_status), hints);
        weather = new WeatherService(this);
        weatherChip = new WeatherChip(findViewById(R.id.weather_chip));
        departureChip = new DepartureChip(findViewById(R.id.departure_chip));
        hero = findViewById(R.id.hero);
        glotze = findViewById(R.id.hero_glotze);
        glotzeOnStage = glotze.getVisibility() == View.VISIBLE;
        screenSaver = new ScreenSaver(this);
        nightMode = new NightMode(this);
        nightShade = findViewById(R.id.night_shade);
        setUpStatusPill();
        setUpCollapsingHero();
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(this::onFocusMoved);
    }

    @Override
    protected void onStart() {
        super.onStart();
        network.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (design != Design.current(this)) {
            // The settings chose the other design while home waited below them.
            recreate();
            return;
        }
        followNight();
        hints.refresh();
        showApps();
        if (focusFirstTileOnResume || getCurrentFocus() == null) {
            focusFirstTileOnResume = false;
            navigation.focusStart();
        }
        weather.load(weatherChip);
        departureChip.start();
        showFocused(getCurrentFocus());
        restartIdleTimer();
        glotze.loop(GlotzeView.Motion.IDLE);
        resumed = true;
    }

    @Override
    protected void onPause() {
        resumed = false;
        idleTimer.removeCallbacks(lockWhenIdle);
        nightShade.removeCallbacks(followNight);
        departureChip.stop();
        glotze.stop();
        super.onPause();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        restartIdleTimer();
    }

    /**
     * Pressing Home returns to the first tile, like tvOS. Coming back from an app, the focus
     * waits for onResume to lay out the current order; when home is already showing, Fire OS
     * delivers the intent without pausing, so the focus moves at once.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (resumed) {
            navigation.focusStart();
        } else {
            focusFirstTileOnResume = true;
        }
    }

    @Override
    protected void onStop() {
        network.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        weather.shutdown();
        sounds.release();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mover.handle(event)
                || handleHomeScreenKey(event)
                || navigation.handle(event, getCurrentFocus())
                || super.dispatchKeyEvent(event);
    }

    @Override
    public void onTileClicked(AppTile tile, View source) {
        sounds.playSelect();
        startUnlocked(tile.key, () -> launch(tile, source));
    }

    @Override
    public void onTileLongPressed(AppTile tile) {
        menu.show(tile);
    }

    /** The dock follows pins and usage, so only the tiles of the grid can be placed by hand. */
    @Override
    public boolean canMoveTiles() {
        return !arrangement.isOrderedByUsage() && !shelf.contains(getCurrentFocus());
    }

    @Override
    public void moveFocusedTile() {
        mover.start();
        Toast.makeText(this, R.string.move_hint, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean isPinned(AppTile tile) {
        return arrangement.isPinned(tile);
    }

    @Override
    public void togglePin(AppTile tile) {
        arrangement.togglePin(tile);
        showApps();
    }

    @Override
    public void hideTile(AppTile tile) {
        arrangement.hide(tile.key);
        showApps();
    }

    @Override
    public boolean isLocked(AppTile tile) {
        return appLock.isLocked(tile.key);
    }

    /** Locking needs a PIN to exist; unlocking needs it to be entered, or anyone could undo the lock. */
    @Override
    public void toggleLock(AppTile tile) {
        if (appLock.isLocked(tile.key)) {
            askForPin(pinDialog::showVerify, () -> appLock.setLocked(tile.key, false));
        } else if (appLock.hasPin()) {
            appLock.setLocked(tile.key, true);
        } else {
            askForPin(pinDialog::showSet, () -> appLock.setLocked(tile.key, true));
        }
    }

    @Override
    public TileCategory categoryOf(AppTile tile) {
        return arrangement.categoryOf(tile);
    }

    @Override
    public void setCategory(AppTile tile, TileCategory category) {
        arrangement.setCategory(tile, category);
        showApps();
    }

    private void launch(AppTile tile, View source) {
        arrangement.recordLaunch(tile);
        starter.startFrom(source, tile.launchIntent, tile.label);
    }

    /** Runs the start at once, or after the PIN when the app is locked. */
    private void startUnlocked(String packageName, Runnable start) {
        if (appLock.isLocked(packageName)) {
            askForPin(pinDialog::showVerify, start);
        } else {
            start.run();
        }
    }

    /** Shows one PIN prompt at a time, so a quick double click cannot stack a second dialog. */
    private void askForPin(Consumer<PinDialog.Callback> show, Runnable afterConfirm) {
        if (pinPromptOpen) {
            return;
        }
        pinPromptOpen = true;
        show.accept(new PinDialog.Callback() {
            @Override
            public void onConfirmed() {
                pinPromptOpen = false;
                afterConfirm.run();
            }

            @Override
            public void onCancelled() {
                // Nothing was changed yet, so there is nothing to undo.
                pinPromptOpen = false;
            }
        });
    }

    /** Only where Fire OS would show its Ambient Experience instead of our screen saver. */
    private void restartIdleTimer() {
        idleTimer.removeCallbacks(lockWhenIdle);
        if (screenSaver.isOwn() && screenSaver.showsOnlyFromHome()) {
            idleTimer.postDelayed(lockWhenIdle, screenSaver.homeIdleMs());
        }
    }

    private void showLockScreen() {
        starter.start(new Intent(this, LockScreenActivity.class), getString(R.string.screen_saver));
    }

    /** The grid only holds the apps the dock does not show, so the dock's apps keep their place at the front. */
    private void saveMovedTiles() {
        arrangement.saveArrangement(shelf.tiles(), grid.arranged());
        showApps();
    }

    /** Every app is shown once: in the dock, or else in the grid, which keeps its sections. */
    private void showApps() {
        arrangement.catchUpOnUsage();
        List<AppTile> visible = arrangement.visibleInOrder(catalog.loadApps());
        List<AppTile> dock = HomeShelf.dockOf(arrangement.inDockOrder(visible));
        Map<TileCategory, List<AppTile>> sections = arrangement.sectionsOf(visible);
        for (List<AppTile> section : sections.values()) {
            section.removeAll(dock);
        }
        grid.show(sections);
        shelf.show(dock);
    }

    private void setUpStatusPill() {
        Resources res = getResources();
        View statusPill = findViewById(R.id.status_bar);
        statusPill.setBackground(SurfaceDrawable.card(this, res.getDimension(R.dimen.stage_pill_corner)));
        findViewById(R.id.settings_button).setOnClickListener(this::openSettings);
    }

    /**
     * The stage fades as it scrolls away. The rows are at least one screen tall, so the stage
     * can always leave completely, however few apps there are.
     */
    private void setUpCollapsingHero() {
        scroller.setOnScrollChangeListener((view, x, y, oldX, oldY) -> fadeHero(y));
        scroller.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (tileRows.getMinimumHeight() != bottom - top) {
                tileRows.setMinimumHeight(bottom - top);
            }
        });
    }

    /** Once the stage has faded out completely, Glotze is hidden so that it stops animating unseen. */
    private void fadeHero(int scrollY) {
        float alpha = Math.max(0f, 1f - scrollY / (float) Math.max(1, hero.getHeight()));
        hero.setAlpha(alpha);
        if (glotzeOnStage) {
            glotze.setVisibility(alpha > 0f ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void openSettings(View source) {
        sounds.playSelect();
        starter.startFrom(source, new Intent(this, SettingsActivity.class), getString(R.string.settings_title));
    }

    private void onFocusMoved(View oldFocus, View newFocus) {
        if (oldFocus != null) {
            sounds.playFocus();
        }
        if (newFocus != null) {
            // The new view may not be laid out yet, so look once it is.
            glotze.post(() -> glotze.lookAt(gazeTowards(newFocus)));
        }
        showFocused(newFocus);
    }

    /** From -1 (far left of Glotze) to +1 (far right), the focused view measured in half screen widths. */
    private float gazeTowards(View target) {
        float halfScreen = getResources().getDisplayMetrics().widthPixels / 2f;
        return (centerXOnScreen(target) - centerXOnScreen(glotze)) / halfScreen;
    }

    private float centerXOnScreen(View view) {
        view.getLocationOnScreen(screenPosition);
        return screenPosition[0] + view.getWidth() / 2f;
    }

    /**
     * The shelf and the settings button live on the stage, so focusing them brings the stage
     * back; a tile of the grid scrolls the stage out of sight.
     */
    private void showFocused(View focused) {
        if (grid.contains(focused)) {
            scroller.glideToReveal(focused, scroller.scrollYToClear(grid.firstRow().get(0)));
        } else if (focused != null) {
            scroller.glideTo(0);
        }
    }

    private boolean handleHomeScreenKey(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                // Home has no "back"; like tvOS, it returns to the first tile instead.
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    navigation.focusStart();
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    showMenuForFocusedTile();
                }
                return true;
            default:
                return false;
        }
    }

    /** The menu key does what a long press does: a tile offers its options. */
    private void showMenuForFocusedTile() {
        View focused = getCurrentFocus();
        if (grid.contains(focused) || shelf.contains(focused)) {
            focused.performLongClick();
        }
    }

    /** Night dimming, and Glotze dozing off, follow the clock while home stays open. */
    private void followNight() {
        boolean night = nightMode.isActive();
        nightShade.animate().alpha(night ? 1f : 0f).setDuration(NIGHT_FADE_MS).start();
        glotze.setAsleep(night);
        nightShade.removeCallbacks(followNight);
        nightShade.postDelayed(followNight, MINUTE_MS);
    }
}
