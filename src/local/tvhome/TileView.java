package local.tvhome;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * One app tile: a 16:9 card that grows when focused (via its state list animator) and shows
 * the lime focus ring over its artwork.
 */
public final class TileView extends LinearLayout {
    private static final float WIGGLE_DEGREES = 1.4f;
    private static final long WIGGLE_HALF_PERIOD_MS = 130;

    private View card;
    private ArtworkFrame artwork;
    private ObjectAnimator wiggle;
    private AppTile tile;

    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** A tile showing the app, reporting clicks and long presses of whatever app it shows by then. */
    static TileView inflate(LayoutInflater inflater, ViewGroup parent, AppTile tile, TileGrid.Listener listener) {
        TileView view = (TileView) inflater.inflate(R.layout.home_tile, parent, false);
        view.bind(tile);
        view.setOnClickListener(clicked -> listener.onTileClicked(view.tile, view));
        view.setOnLongClickListener(pressed -> {
            listener.onTileLongPressed(view.tile);
            return true;
        });
        return view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        card = findViewById(R.id.card);
        artwork = new ArtworkFrame(card, findViewById(R.id.artwork), res.getDimensionPixelSize(R.dimen.icon_inset));
        findViewById(R.id.ring).setBackground(
                SurfaceDrawable.focusRing(getContext(), res.getDimension(R.dimen.stage_tile_corner)));
    }

    void bind(AppTile tile) {
        this.tile = tile;
        artwork.show(tile);
        setContentDescription(tile.label);
    }

    AppTile tile() {
        return tile;
    }

    void startWiggle() {
        wiggle = ObjectAnimator.ofFloat(card, View.ROTATION, -WIGGLE_DEGREES, WIGGLE_DEGREES);
        wiggle.setDuration(WIGGLE_HALF_PERIOD_MS);
        wiggle.setRepeatCount(ValueAnimator.INFINITE);
        wiggle.setRepeatMode(ValueAnimator.REVERSE);
        wiggle.start();
    }

    void stopWiggle() {
        if (wiggle != null) {
            wiggle.cancel();
            wiggle = null;
        }
        card.setRotation(0f);
    }
}
