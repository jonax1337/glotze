package local.tvhome;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The row on the stage, always in the same place: a dock of the first apps without a heading, so
 * the page never looks empty. Its cells sit on the same five columns as the grid below. The "All
 * apps" pill hangs under the row. When there is no app at all, a note takes their place.
 */
final class HomeShelf {
    interface Listener extends TileGrid.Listener {
        void onAllAppsClicked();
    }

    static final int ITEM_LIMIT = TileGrid.COLUMNS;

    private final LinearLayout row;
    private final View allAppsPill;
    private final View emptyNote;
    private final LayoutInflater inflater;
    private final Listener listener;
    /** The app tiles as shown, from left to right. */
    private final List<TileView> views = new ArrayList<>();

    /** Finds the row, the pill and the empty note inside the home content. */
    HomeShelf(View home, Listener listener) {
        this.row = home.findViewById(R.id.shelf_row);
        this.allAppsPill = home.findViewById(R.id.all_apps_pill);
        this.emptyNote = home.findViewById(R.id.home_empty);
        this.inflater = LayoutInflater.from(home.getContext());
        this.listener = listener;
        TileGrid.alignToColumns(row);
        styleAllAppsPill();
    }

    /** Shows the first of the apps in dock order. */
    void show(List<AppTile> appsInDockOrder) {
        List<AppTile> dock = new ArrayList<>(
                appsInDockOrder.subList(0, Math.min(appsInDockOrder.size(), ITEM_LIMIT)));
        emptyNote.setVisibility(dock.isEmpty() ? View.VISIBLE : View.GONE);
        allAppsPill.setVisibility(dock.isEmpty() ? View.GONE : View.VISIBLE);
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

    /** The pill below the row; it leads to the grid and is only visible while the dock has apps. */
    View allAppsPill() {
        return allAppsPill;
    }

    private void styleAllAppsPill() {
        Resources res = allAppsPill.getResources();
        float corner = res.getDimension(R.dimen.stage_chip_corner);
        allAppsPill.setBackground(SurfaceDrawable.card(res, corner));
        allAppsPill.findViewById(R.id.all_apps_ring).setBackground(SurfaceDrawable.focusRing(res, corner));
        allAppsPill.setOnClickListener(clicked -> listener.onAllAppsClicked());
    }

    private static List<String> keysOf(List<AppTile> dock) {
        List<String> keys = new ArrayList<>();
        for (AppTile tile : dock) {
            keys.add(tile.key);
        }
        return keys;
    }

    private List<String> shownKeys() {
        List<String> keys = new ArrayList<>();
        for (TileView view : views) {
            keys.add(view.tile().key);
        }
        return keys;
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
