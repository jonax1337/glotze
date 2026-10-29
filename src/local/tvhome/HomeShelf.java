package local.tvhome;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The row on the stage, always in the same place: a dock of the first apps without a heading, so
 * the page never looks empty. Its cells sit on the same five columns as the grid below, which
 * shows the remaining apps and peeks out under the dock. When there is no app at all, a note
 * takes its place.
 */
final class HomeShelf {
    static final int ITEM_LIMIT = TileGrid.COLUMNS;

    private final LinearLayout row;
    private final View emptyNote;
    private final LayoutInflater inflater;
    private final TileGrid.Listener listener;
    /** The app tiles as shown, from left to right. */
    private final List<TileView> views = new ArrayList<>();

    /** Finds the row and the empty note inside the home content. */
    HomeShelf(View home, TileGrid.Listener listener) {
        this.row = home.findViewById(R.id.shelf_row);
        this.emptyNote = home.findViewById(R.id.home_empty);
        this.inflater = LayoutInflater.from(home.getContext());
        this.listener = listener;
        TileGrid.alignToColumns(row);
    }

    /** The apps the dock shows out of all apps in dock order; the grid shows the others. */
    static List<AppTile> dockOf(List<AppTile> appsInDockOrder) {
        return new ArrayList<>(appsInDockOrder.subList(0, Math.min(appsInDockOrder.size(), ITEM_LIMIT)));
    }

    /** Shows the dock, see {@link #dockOf}. */
    void show(List<AppTile> dock) {
        emptyNote.setVisibility(dock.isEmpty() ? View.VISIBLE : View.GONE);
        if (keysOf(dock).equals(shownKeys())) {
            bindAll(dock);
        } else {
            rebuild(dock);
        }
    }

    boolean contains(View view) {
        return views.contains(view);
    }

    /** The app tiles from left to right. */
    List<? extends View> views() {
        return Collections.unmodifiableList(views);
    }

    /** The apps as shown, from left to right. */
    List<AppTile> tiles() {
        List<AppTile> tiles = new ArrayList<>();
        for (TileView view : views) {
            tiles.add(view.tile());
        }
        return tiles;
    }

    private static List<String> keysOf(List<AppTile> dock) {
        List<String> keys = new ArrayList<>();
        for (AppTile tile : dock) {
            keys.add(tile.key);
        }
        return keys;
    }

    private List<String> shownKeys() {
        return keysOf(tiles());
    }

    /** The same cells in the same order: only their texts and pictures may have changed. */
    private void bindAll(List<AppTile> dock) {
        for (int i = 0; i < dock.size(); i++) {
            views.get(i).bind(dock.get(i));
        }
    }

    /** Builds the cells anew; the focus stays in the row on the same app, or else goes to the first. */
    private void rebuild(List<AppTile> dock) {
        View focused = row.findFocus();
        String focusedKey = focused == null ? null : ((TileView) focused).tile().key;
        row.removeAllViews();
        views.clear();
        for (AppTile tile : dock) {
            TileView view = TileView.inflate(inflater, row, tile, listener);
            row.addView(view);
            views.add(view);
        }
        if (focusedKey != null) {
            focusAppOrFirst(focusedKey);
        }
    }

    private void focusAppOrFirst(String key) {
        for (TileView view : views) {
            if (view.tile().key.equals(key)) {
                view.requestFocus();
                return;
            }
        }
        if (!views.isEmpty()) {
            views.get(0).requestFocus();
        }
    }
}
