package local.tvhome;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.List;

/**
 * Steers the D-pad across the home screen: the settings button above, the shelf, and the grid.
 * The system's focus search knows nothing of the layout's intent, so it used to leap from the end
 * of a row to a distant view. Here every move is spelled out: sideways and down never leave the
 * area they start in except by the rules below, and at the edge nothing moves but a bump.
 * <ul>
 * <li>Shelf: left and right step along the row, down enters the grid in the column that overlaps
 * most, up goes to the settings button.</li>
 * <li>Grid: the system's search moves inside it, sideways only within the row; up from the first
 * row returns to the shelf.</li>
 * <li>Settings button: down goes to the first cell of the shelf.</li>
 * </ul>
 */
final class HomeNavigation {
    static final int NO_DIRECTION = 0;

    private final HomeShelf shelf;
    private final TileGrid grid;
    private final ViewGroup header;
    private final View settingsButton;
    private final int[] position = new int[2];

    HomeNavigation(HomeShelf shelf, TileGrid grid, ViewGroup header, View settingsButton) {
        this.shelf = shelf;
        this.grid = grid;
        this.header = header;
        this.settingsButton = settingsButton;
    }

    /** The {@code View.FOCUS_*} constant for a D-pad key, or {@link #NO_DIRECTION}. */
    static int focusDirectionOf(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return View.FOCUS_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return View.FOCUS_RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP:
                return View.FOCUS_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return View.FOCUS_DOWN;
            default:
                return NO_DIRECTION;
        }
    }

    /** Moves the focus for a D-pad press that starts on the shelf, the grid or the header; others are left alone. */
    boolean handle(KeyEvent event, View focused) {
        int direction = focusDirectionOf(event.getKeyCode());
        if (direction == NO_DIRECTION || event.getAction() != KeyEvent.ACTION_DOWN || !isSteered(focused)) {
            return false;
        }
        View target = targetFrom(focused, direction);
        if (target != null) {
            target.requestFocus();
        } else {
            EdgeBump.play(focused, direction);
        }
        return true;
    }

    /** Gives the focus to the first cell of the shelf, or else to the first tile of the grid. */
    void focusStart() {
        View start = firstCellOrNull();
        if (start != null) {
            start.requestFocus();
        }
    }

    private boolean isSteered(View view) {
        return shelf.contains(view) || grid.contains(view) || isInHeader(view);
    }

    private boolean isInHeader(View view) {
        if (view == null) {
            return false;
        }
        for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == header) {
                return true;
            }
        }
        return false;
    }

    private View targetFrom(View focused, int direction) {
        if (shelf.contains(focused)) {
            return fromShelf(focused, direction);
        }
        if (grid.contains(focused)) {
            return fromGrid(focused, direction);
        }
        return fromHeader(focused, direction);
    }

    private View fromShelf(View focused, int direction) {
        List<View> cells = shelf.views();
        int index = cells.indexOf(focused);
        switch (direction) {
            case View.FOCUS_LEFT:
                return index > 0 ? cells.get(index - 1) : null;
            case View.FOCUS_RIGHT:
                return index < cells.size() - 1 ? cells.get(index + 1) : null;
            case View.FOCUS_DOWN:
                return mostOverlapping(grid.firstRow(), focused);
            default:
                return settingsButton;
        }
    }

    private View fromGrid(View focused, int direction) {
        boolean leavesUpwards = direction == View.FOCUS_UP && grid.firstRow().contains(focused);
        if (leavesUpwards) {
            return mostOverlapping(shelf.views(), focused);
        }
        View found = focused.focusSearch(direction);
        if (!grid.contains(found)) {
            return null;
        }
        boolean sideways = direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT;
        // Every grid row is a layout of its own; a sideways move that lands in another row has wrapped around.
        return sideways && found.getParent() != focused.getParent() ? null : found;
    }

    private View fromHeader(View focused, int direction) {
        if (direction == View.FOCUS_DOWN) {
            return firstCellOrNull();
        }
        View found = focused.focusSearch(direction);
        return isInHeader(found) ? found : null;
    }

    private View firstCellOrNull() {
        List<View> cells = shelf.views();
        if (!cells.isEmpty()) {
            return cells.get(0);
        }
        List<View> firstRow = grid.firstRow();
        return firstRow.isEmpty() ? null : firstRow.get(0);
    }

    /** The candidate sharing the widest stretch of the reference's columns; the nearest one if none overlaps. */
    private View mostOverlapping(List<View> candidates, View reference) {
        View best = null;
        int bestOverlap = Integer.MIN_VALUE;
        int referenceStart = leftOnScreen(reference);
        int referenceEnd = referenceStart + reference.getWidth();
        for (View candidate : candidates) {
            int start = leftOnScreen(candidate);
            int overlap = Math.min(referenceEnd, start + candidate.getWidth()) - Math.max(referenceStart, start);
            if (overlap > bestOverlap) {
                best = candidate;
                bestOverlap = overlap;
            }
        }
        return best;
    }

    private int leftOnScreen(View view) {
        view.getLocationOnScreen(position);
        return position[0];
    }
}
