package local.tvhome;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.media.MediaMetadata;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Optional;

/** Song and cover from whatever app is playing; hidden while nothing plays. */
final class NowPlayingWidget implements LockWidget, MediaWatcher.Listener {
    /** The cover sits in the pill's rounded end as a disc. */
    private static final ViewOutlineProvider ROUND = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    };

    private final View pill;
    private final ImageView cover;
    private final TextView title;
    private final TextView artist;
    private final MediaWatcher watcher;

    NowPlayingWidget(LayoutInflater inflater, ViewGroup parent) {
        pill = inflater.inflate(R.layout.lock_widget_now_playing, parent, false);
        cover = pill.findViewById(R.id.now_playing_cover);
        title = pill.findViewById(R.id.now_playing_title);
        artist = pill.findViewById(R.id.now_playing_artist);
        Resources res = pill.getResources();
        pill.setBackground(SurfaceDrawable.card(pill.getContext(), res.getDimension(R.dimen.lock_now_playing_height) / 2f));
        cover.setOutlineProvider(ROUND);
        cover.setClipToOutline(true);
        watcher = new MediaWatcher(pill.getContext(), this);
    }

    @Override
    public View view() {
        return pill;
    }

    @Override
    public void start() {
        watcher.start();
    }

    @Override
    public void stop() {
        watcher.stop();
    }

    @Override
    public void onPlaying(MediaMetadata track) {
        title.setText(track.getText(MediaMetadata.METADATA_KEY_TITLE));
        artist.setText(track.getText(MediaMetadata.METADATA_KEY_ARTIST));
        Optional<Bitmap> artwork = MediaWatcher.artworkOf(track);
        cover.setVisibility(artwork.isPresent() ? View.VISIBLE : View.GONE);
        artwork.ifPresent(cover::setImageBitmap);
        pill.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopped() {
        pill.setVisibility(View.GONE);
    }
}
