package local.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One followed series in the width of a grid column: the poster (2:3, full height) on the left,
 * then title and episode, and a lime "new" badge on the poster while an episode waits unwatched.
 * It grows when focused and shows the lime focus ring, like an app tile.
 */
public final class SeriesCardView extends LinearLayout {
    private static final float POSTER_WIDTH_PER_HEIGHT = 2f / 3f;

    private View card;
    private View posterFrame;
    private ImageView poster;
    private TextView badge;
    private TextView title;
    private TextView subtitle;
    private SeriesCard series;

    public SeriesCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        card = findViewById(R.id.card);
        posterFrame = findViewById(R.id.poster_frame);
        poster = findViewById(R.id.poster);
        badge = findViewById(R.id.badge);
        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        float corner = res.getDimension(R.dimen.stage_tile_corner);
        card.setBackground(SurfaceDrawable.card(res, corner));
        card.setClipToOutline(true);
        findViewById(R.id.ring).setBackground(SurfaceDrawable.focusRing(res, corner));
        badge.setBackground(limeBadge(res));
    }

    /**
     * The poster is as wide as 2:3 of the card's height, which only the card knows. It is measured
     * once on its own, with a different height spec than the real pass uses, so the real pass
     * never reuses a stale measurement, and the poster's width is set before that single pass.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int cardWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        card.measure(
                MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        posterFrame.getLayoutParams().width = Math.round(card.getMeasuredHeight() * POSTER_WIDTH_PER_HEIGHT);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    void bind(SeriesCard newSeries) {
        boolean posterChanged = series == null || !sameText(series.posterUrl, newSeries.posterUrl);
        series = newSeries;
        title.setText(newSeries.title);
        subtitle.setText(newSeries.subtitle);
        subtitle.setVisibility(newSeries.subtitle.isEmpty() ? GONE : VISIBLE);
        badge.setVisibility(newSeries.fresh ? VISIBLE : GONE);
        setContentDescription(describe(newSeries));
        if (posterChanged) {
            showPoster(newSeries.posterUrl);
        }
    }

    SeriesCard series() {
        return series;
    }

    /** Until the picture arrives, or when there is none, the flat raised fill of the frame shows. */
    private void showPoster(String url) {
        poster.setImageDrawable(null);
        if (url == null) {
            return;
        }
        PosterCache.load(getContext(), url, bitmap -> showLoadedPoster(url, bitmap));
    }

    /** A card is re-bound while a picture loads, so a late picture must still belong to the card's series. */
    private void showLoadedPoster(String url, Bitmap bitmap) {
        if (bitmap != null && series != null && url.equals(series.posterUrl)) {
            poster.setImageBitmap(bitmap);
        }
    }

    private String describe(SeriesCard shown) {
        return shown.subtitle.isEmpty()
                ? shown.title
                : getResources().getString(R.string.series_card_description, shown.title, shown.subtitle);
    }

    private static boolean sameText(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private static GradientDrawable limeBadge(Resources res) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(res.getColor(R.color.glotze_lime, null));
        shape.setCornerRadius(res.getDimension(R.dimen.stage_series_badge_corner));
        return shape;
    }
}
