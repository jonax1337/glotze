package local.tvhome;

import android.view.KeyEvent;

/**
 * Lets the user rearrange tiles with the D-pad while the moving tile wiggles; OK or Back ends it.
 * A tile only trades places inside its own category section.
 */
final class TileMover {
    private final TileGrid grid;
    private final Runnable onFinished;
    private int movingIndex = -1;

    TileMover(TileGrid grid, Runnable onFinished) {
        this.grid = grid;
        this.onFinished = onFinished;
    }

    void start() {
        movingIndex = grid.focusedIndex();
        if (isActive()) {
            grid.startWiggleAt(movingIndex);
        }
    }

    /** Returns whether the key was consumed by an ongoing move. */
    boolean handle(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!isActive() || !isMoverKey(keyCode)) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            react(keyCode);
        }
        return true;
    }

    private boolean isActive() {
        return movingIndex >= 0;
    }

    private void react(int keyCode) {
        if (isFinishKey(keyCode)) {
            finish();
        } else {
            moveTowards(HomeNavigation.focusDirectionOf(keyCode));
        }
    }

    private void moveTowards(int focusDirection) {
        int target = grid.neighbourIndex(movingIndex, focusDirection);
        if (target < 0) {
            return;
        }
        grid.stopWiggleAt(movingIndex);
        grid.swap(movingIndex, target);
        grid.focus(target);
        grid.startWiggleAt(target);
        movingIndex = target;
    }

    private void finish() {
        grid.stopWiggleAt(movingIndex);
        movingIndex = -1;
        onFinished.run();
    }

    private static boolean isMoverKey(int keyCode) {
        return isFinishKey(keyCode) || HomeNavigation.focusDirectionOf(keyCode) != HomeNavigation.NO_DIRECTION;
    }

    private static boolean isFinishKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_BACK;
    }
}
