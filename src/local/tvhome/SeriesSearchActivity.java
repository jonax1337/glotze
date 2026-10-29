package local.tvhome;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Follows series: lists the followed ones and searches TVmaze for more. */
public final class SeriesSearchActivity extends Activity {
    private static final String TAG = "SeriesSearchActivity";
    private static final long DEBOUNCE_MS = 500;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ShowHit> found = new ArrayList<>();
    private final Runnable debouncedSearch = this::startSearch;
    private SeriesStore store;
    private UiSounds sounds;
    private Fonts fonts;
    private FocusScrollView scroller;
    private LinearLayout rows;
    private LinearLayout followedRows;
    private LinearLayout foundRows;
    private TextView noSeriesNote;
    private TextView searchStatus;
    private EditText searchField;
    /** Counts the searches, so that the answer to an outdated one is ignored. */
    private int searchNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        fonts = new Fonts(getAssets());
        sounds = new UiSounds(this);
        store = new SeriesStore(this);
        scroller = findViewById(R.id.settings_scroller);
        rows = findViewById(R.id.settings_rows);
        dressInSoftSolid();
        ((TextView) findViewById(R.id.settings_device)).setText(R.string.series_subtitle);
        addFollowedSection();
        addSearchSection();
        addAttribution();
        fonts.applyTo(findViewById(android.R.id.content));
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(this::onFocusMoved);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(debouncedSearch);
        worker.shutdownNow();
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

    private void addFollowedSection() {
        addHeading(R.string.series_followed_heading);
        followedRows = newColumn();
        noSeriesNote = addNote();
        noSeriesNote.setText(R.string.series_none_followed);
        showFollowed();
    }

    private void addSearchSection() {
        addHeading(R.string.series_search_heading);
        searchField = addSearchField();
        searchStatus = addNote();
        foundRows = newColumn();
    }

    private void addAttribution() {
        TextView attribution = addNote();
        attribution.setText(R.string.series_attribution);
        attribution.setVisibility(View.VISIBLE);
    }

    private void addHeading(int textRes) {
        TextView heading = (TextView) getLayoutInflater().inflate(R.layout.settings_heading, rows, false);
        heading.setText(textRes);
        rows.addView(heading);
    }

    /** A column of rows that is rebuilt as a whole; it is added to the list right away. */
    private LinearLayout newColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        rows.addView(column, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return column;
    }

    /** A line of small text below the rows; hidden until it has something to say. */
    private TextView addNote() {
        Resources res = getResources();
        int inset = res.getDimensionPixelSize(R.dimen.series_note_inset);
        TextView note = new TextView(this);
        note.setTextColor(getColor(R.color.text_muted));
        note.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.series_note_text));
        note.setPadding(inset, 0, inset, res.getDimensionPixelSize(R.dimen.series_note_gap));
        note.setVisibility(View.GONE);
        rows.addView(note);
        return note;
    }

    private EditText addSearchField() {
        Resources res = getResources();
        int inset = res.getDimensionPixelSize(R.dimen.series_field_inset);
        EditText field = new EditText(this);
        field.setBackground(SurfaceDrawable.focusableCard(res, res.getDimension(R.dimen.surface_tile_corner)));
        field.setPadding(inset, 0, inset, 0);
        field.setHint(R.string.series_search_hint);
        field.setTextColor(getColor(R.color.text_primary));
        field.setHintTextColor(getColor(R.color.text_muted));
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.series_field_text));
        field.setSingleLine();
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return false;
            }
            startSearch();
            return true;
        });
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                handler.removeCallbacks(debouncedSearch);
                handler.postDelayed(debouncedSearch, DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, res.getDimensionPixelSize(R.dimen.row_height));
        params.bottomMargin = res.getDimensionPixelSize(R.dimen.series_row_gap);
        rows.addView(field, params);
        return field;
    }

    private View seriesRow(String name, String meta, String posterUrl, int stateRes, Runnable action) {
        Resources res = getResources();
        View row = getLayoutInflater().inflate(R.layout.series_search_row, rows, false);
        row.setBackground(SurfaceDrawable.focusableCard(res, res.getDimension(R.dimen.surface_tile_corner)));
        ((TextView) row.findViewById(R.id.series_name)).setText(name);
        ((TextView) row.findViewById(R.id.series_meta)).setText(meta);
        ((TextView) row.findViewById(R.id.series_state)).setText(stateRes);
        showPoster((ImageView) row.findViewById(R.id.series_poster), posterUrl);
        row.setOnClickListener(view -> {
            sounds.playSelect();
            action.run();
        });
        fonts.applyTo(row);
        return row;
    }

    private void showPoster(ImageView poster, String url) {
        GradientDrawable placeholder = new GradientDrawable();
        placeholder.setColor(getColor(R.color.surface_card_raised));
        placeholder.setCornerRadius(getResources().getDimension(R.dimen.series_poster_corner));
        poster.setBackground(placeholder);
        if (url == null) {
            return;
        }
        PosterCache.load(this, url, bitmap -> {
            if (bitmap != null && !isDestroyed()) {
                poster.setImageBitmap(bitmap);
            }
        });
    }

    private void onFocusMoved(View oldFocus, View newFocus) {
        if (newFocus != null) {
            scroller.glideToReveal(newFocus);
        }
        if (oldFocus != null) {
            sounds.playFocus();
        }
    }

    private void showFollowed() {
        followedRows.removeAllViews();
        for (Series series : store.all()) {
            String meta = series.networkName != null ? series.networkName : "";
            followedRows.addView(seriesRow(series.name, meta, series.imageUrl,
                    R.string.series_remove, () -> unfollowFromList(series.id)));
        }
        noSeriesNote.setVisibility(followedRows.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void showFound() {
        foundRows.removeAllViews();
        for (ShowHit hit : found) {
            int stateRes = store.isFollowed(hit.id) ? R.string.series_following : R.string.series_follow;
            foundRows.addView(seriesRow(hit.name, metaOf(hit), hit.imageUrl, stateRes, () -> toggleFollow(hit)));
        }
    }

    private String metaOf(ShowHit hit) {
        if (hit.premieredYear == 0) {
            return hit.networkName != null ? hit.networkName : "";
        }
        if (hit.networkName == null) {
            return getString(R.string.series_meta_year, hit.premieredYear);
        }
        return getString(R.string.series_meta_year_network, hit.premieredYear, hit.networkName);
    }

    private void toggleFollow(ShowHit hit) {
        int index = foundRows.indexOfChild(foundRows.getFocusedChild());
        if (store.isFollowed(hit.id)) {
            store.unfollow(hit.id);
        } else {
            store.follow(hit, false);
        }
        showFollowed();
        showFound();
        focusNear(foundRows, index, searchField);
    }

    private void unfollowFromList(long id) {
        int index = followedRows.indexOfChild(followedRows.getFocusedChild());
        store.unfollow(id);
        showFollowed();
        showFound();
        focusNear(followedRows, index, searchField);
    }

    /** Focus would be lost with the row that held it; it moves to the row now at its place. */
    private static void focusNear(ViewGroup column, int index, View fallback) {
        int count = column.getChildCount();
        View target = count > 0 ? column.getChildAt(Math.min(Math.max(index, 0), count - 1)) : fallback;
        target.requestFocus();
    }

    private void startSearch() {
        handler.removeCallbacks(debouncedSearch);
        String query = searchField.getText().toString().trim();
        int search = ++searchNumber;
        found.clear();
        showFound();
        if (query.isEmpty()) {
            searchStatus.setVisibility(View.GONE);
            return;
        }
        showStatus(R.string.series_searching);
        worker.execute(() -> runSearch(search, query));
    }

    private void runSearch(int search, String query) {
        try {
            List<ShowHit> hits = TvMazeClient.search(query);
            runOnUiThread(() -> onFound(search, hits));
        } catch (IOException e) {
            Log.w(TAG, "Series search for \"" + query + "\" failed", e);
            runOnUiThread(() -> onSearchFailed(search, R.string.series_search_offline));
        } catch (RuntimeException e) {
            Log.w(TAG, "Series search for \"" + query + "\" answered something unreadable", e);
            runOnUiThread(() -> onSearchFailed(search, R.string.series_search_unreadable));
        }
    }

    private void onFound(int search, List<ShowHit> hits) {
        if (search != searchNumber || isDestroyed()) {
            return;
        }
        found.addAll(hits);
        showFound();
        if (found.isEmpty()) {
            showStatus(R.string.series_no_results);
            return;
        }
        searchStatus.setVisibility(View.GONE);
    }

    private void onSearchFailed(int search, int messageRes) {
        if (search == searchNumber && !isDestroyed()) {
            showStatus(messageRes);
        }
    }

    private void showStatus(int textRes) {
        searchStatus.setText(textRes);
        searchStatus.setVisibility(View.VISIBLE);
    }
}
