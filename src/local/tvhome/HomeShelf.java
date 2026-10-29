package local.tvhome;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The row on the stage, always in the same place. While series are followed it is headed
 * "Continue watching" and shows their cards; otherwise it is a dock of apps without a heading, so
 * the page never looks empty. Either way the row ends with the "All apps" tile, and its cells sit
 * on the same six columns as the grid below. When the row has nothing to show at all, a note
 * takes its place.
 */
final class HomeShelf {
    interface Listener extends TileGrid.Listener {
        void onSeriesClicked(SeriesCard card, View source);

        void onSeriesLongPressed(SeriesCard card);

        void onAllAppsClicked();
    }

    /** One column is left for the "All apps" tile. */
    static final int ITEM_LIMIT = TileGrid.COLUMNS - 1;
    private static final String SERIES_KEY_PREFIX = "series:";
    private static final String APP_KEY_PREFIX = "app:";
    private static final String ALL_APPS_KEY = "all";

    private final View label;
    private final LinearLayout row;
    private final TextView caption;
    private final View emptyNote;
    private final LayoutInflater inflater;
    private final Fonts fonts;
    private final Listener listener;
    /** The cells as shown: series cards or app tiles, then the "All apps" tile. */
    private final List<View> views = new ArrayList<>();
    private List<SeriesCard> series = Collections.emptyList();
    private List<AppTile> dock = Collections.emptyList();
    /** The app tile whose name the caption is about to show, or null while it is hidden. */
    private View captioned;

    /** Finds the row, its heading, the caption and the empty note inside the home content. */
    HomeShelf(View home, Fonts fonts, Listener listener) {
        this.label = home.findViewById(R.id.shelf_label);
        this.row = home.findViewById(R.id.shelf_row);
        this.caption = home.findViewById(R.id.shelf_caption);
        this.emptyNote = home.findViewById(R.id.home_empty);
        this.inflater = LayoutInflater.from(home.getContext());
        this.fonts = fonts;
        this.listener = listener;
        TileGrid.alignToColumns(row);
    }

    /** Shows the series, or else the first of the apps in dock order. */
    void show(List<SeriesCard> newSeries, List<AppTile> appsInDockOrder) {
        series = firstItems(newSeries);
        dock = firstItems(appsInDockOrder);
        render();
    }

    void setSeries(List<SeriesCard> newSeries) {
        series = firstItems(newSeries);
        render();
    }

    boolean contains(View view) {
        return views.contains(view);
    }

    /** The cells from left to right, the "All apps" tile last. */
    List<View> views() {
        return Collections.unmodifiableList(views);
    }

    /** Names the focused app tile under its card; for any other view the name goes away. */
    void nameFocused(View focused) {
        if (!(focused instanceof TileView) || !contains(focused)) {
            captioned = null;
            caption.setVisibility(View.INVISIBLE);
            return;
        }
        captioned = focused;
        caption.setText(((TileView) focused).tile().label);
        // The tile may not be laid out yet, so place the name once it is.
        caption.post(() -> placeCaptionUnder(focused));
    }

    private void placeCaptionUnder(View tileView) {
        if (captioned == tileView) {
            caption.setTranslationX(row.getLeft() + tileView.getLeft() + tileView.getPaddingLeft());
            caption.setVisibility(View.VISIBLE);
        }
    }

    private static <T> List<T> firstItems(List<T> items) {
        return new ArrayList<>(items.subList(0, Math.min(items.size(), ITEM_LIMIT)));
    }

    private void render() {
        boolean showsSeries = !series.isEmpty();
        List<AppTile> shownApps = showsSeries ? Collections.<AppTile>emptyList() : dock;
        label.setVisibility(showsSeries ? View.VISIBLE : View.GONE);
        emptyNote.setVisibility(showsSeries || !dock.isEmpty() ? View.GONE : View.VISIBLE);
        if (wantedKeys(shownApps).equals(shownKeys())) {
            bindAll(shownApps);
        } else {
            rebuild(shownApps);
        }
    }

    /** The "All apps" tile leads to the grid, so it is only there while the grid has apps. */
    private List<String> wantedKeys(List<AppTile> shownApps) {
        List<String> keys = new ArrayList<>();
        for (SeriesCard card : series) {
            keys.add(SERIES_KEY_PREFIX + card.id);
        }
        for (AppTile tile : shownApps) {
            keys.add(APP_KEY_PREFIX + tile.key);
        }
        if (!dock.isEmpty()) {
            keys.add(ALL_APPS_KEY);
        }
        return keys;
    }

    private List<String> shownKeys() {
        List<String> keys = new ArrayList<>();
        for (View view : views) {
            keys.add(keyOf(view));
        }
        return keys;
    }

    private static String keyOf(View view) {
        if (view instanceof SeriesCardView) {
            return SERIES_KEY_PREFIX + ((SeriesCardView) view).series().id;
        }
        if (view instanceof TileView) {
            return APP_KEY_PREFIX + ((TileView) view).tile().key;
        }
        return ALL_APPS_KEY;
    }

    /** The same cells in the same order: only their texts and pictures may have changed. */
    private void bindAll(List<AppTile> shownApps) {
        for (int i = 0; i < series.size(); i++) {
            ((SeriesCardView) views.get(i)).bind(series.get(i));
        }
        for (int i = 0; i < shownApps.size(); i++) {
            ((TileView) views.get(i)).bind(shownApps.get(i));
        }
    }

    /** Builds the cells anew; the focus stays in the row on the same cell, or else goes to the first. */
    private void rebuild(List<AppTile> shownApps) {
        View focused = row.findFocus();
        String focusedKey = focused == null ? null : keyOf(focused);
        row.removeAllViews();
        views.clear();
        for (SeriesCard card : series) {
            add(newSeriesCard(card));
        }
        for (AppTile tile : shownApps) {
            add(TileView.inflate(inflater, row, tile, listener));
        }
        if (!dock.isEmpty()) {
            add(newAllAppsTile());
        }
        if (focusedKey != null) {
            focusCellOrFirst(focusedKey);
        }
    }

    private void add(View cell) {
        row.addView(cell);
        views.add(cell);
    }

    private void focusCellOrFirst(String key) {
        for (View view : views) {
            if (keyOf(view).equals(key)) {
                view.requestFocus();
                return;
            }
        }
        if (!views.isEmpty()) {
            views.get(0).requestFocus();
        }
    }

    private SeriesCardView newSeriesCard(SeriesCard card) {
        SeriesCardView view = (SeriesCardView) inflater.inflate(R.layout.series_card, row, false);
        fonts.applyTo(view);
        view.bind(card);
        view.setOnClickListener(clicked -> listener.onSeriesClicked(view.series(), view));
        view.setOnLongClickListener(pressed -> {
            listener.onSeriesLongPressed(view.series());
            return true;
        });
        return view;
    }

    private View newAllAppsTile() {
        View tile = inflater.inflate(R.layout.home_end_tile, row, false);
        Resources res = tile.getResources();
        float corner = res.getDimension(R.dimen.stage_tile_corner);
        tile.findViewById(R.id.card).setBackground(SurfaceDrawable.card(res, corner));
        tile.findViewById(R.id.ring).setBackground(SurfaceDrawable.focusRing(res, corner));
        fonts.applyTo(tile);
        tile.setOnClickListener(clicked -> listener.onAllAppsClicked());
        return tile;
    }
}
