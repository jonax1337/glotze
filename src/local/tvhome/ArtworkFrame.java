package local.tvhome;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

/** Shows app artwork in a rounded card: banners fill it, icons sit on a flat fill of their own colour. */
final class ArtworkFrame {
    private final View card;
    private final ImageView image;
    private final int iconInset;
    private final float cornerRadius;

    ArtworkFrame(View card, ImageView image, int iconInset) {
        this.card = card;
        this.image = image;
        this.iconInset = iconInset;
        this.cornerRadius = card.getResources().getDimension(R.dimen.stage_tile_corner);
        card.setClipToOutline(true);
    }

    void show(AppTile tile) {
        image.setImageDrawable(independentCopy(tile.artwork));
        if (tile.hasBanner) {
            showBanner();
        } else {
            showIcon(tile.artwork);
        }
    }

    private void showBanner() {
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setPadding(0, 0, 0, 0);
        card.setBackgroundResource(R.drawable.card);
    }

    private void showIcon(Drawable icon) {
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(iconInset, iconInset, iconInset, iconInset);
        card.setBackground(ArtworkSampler.iconBackground(icon, cornerRadius));
    }

    /** A drawable keeps one set of bounds, so every view that shows artwork needs its own instance. */
    private static Drawable independentCopy(Drawable drawable) {
        Drawable.ConstantState state = drawable.getConstantState();
        return state != null ? state.newDrawable().mutate() : drawable;
    }
}
