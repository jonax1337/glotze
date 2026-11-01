package local.tvhome;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One app tile: a 16:9 card that grows when focused (via its state list animator) and shows
 * the focus ring over its artwork. In Glas it swings in from the way the focus came, leaning and
 * settling like the tiles of tvOS, and its name appears beneath it.
 */
public final class TileView extends LinearLayout {
    private static final float WIGGLE_DEGREES = 1.4f;
    private static final long WIGGLE_HALF_PERIOD_MS = 130;
    private static final Interpolator SETTLE = new DecelerateInterpolator(2f);

    private View card;
    private ArtworkFrame artwork;
    private TextView label;
    private FocusTilt tilt;
    private ObjectAnimator wiggle;
    private AppTile tile;

    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** A tile showing the app, reporting clicks and long presses of whatever app it shows by then. */
    static TileView inflate(LayoutInflater inflater, ViewGroup parent, AppTile tile, Fonts fonts,
            TileGrid.Listener listener) {
        TileView view = (TileView) inflater.inflate(R.layout.home_tile, parent, false);
        fonts.applyTo(view);
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
        label = findViewById(R.id.label);
        tilt = new FocusTilt(getContext());
        View ring = findViewById(R.id.ring);
        ring.setBackground(SurfaceDrawable.tileFace(getContext(), res.getDimension(R.dimen.stage_tile_corner)));
    }

    void bind(AppTile tile) {
        this.tile = tile;
        artwork.show(tile);
        label.setText(tile.label);
        setContentDescription(tile.label);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            swingIn(direction);
        }
    }

    /** Starts leaning towards the move that brought the focus here, then settles level. */
    private void swingIn(int direction) {
        card.setRotationX(tilt.rotationX(direction));
        card.setRotationY(tilt.rotationY(direction));
        card.animate()
                .rotationX(0f)
                .rotationY(0f)
                .setDuration(getResources().getInteger(R.integer.focus_in_ms))
                .setInterpolator(SETTLE)
                .start();
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
