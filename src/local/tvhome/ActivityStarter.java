package local.tvhome;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

final class ActivityStarter {
    private final Context context;

    ActivityStarter(Context context) {
        this.context = context;
    }

    void start(Intent intent, CharSequence label) {
        startWithOptions(intent, Bundle.EMPTY, label);
    }

    /** Starts the activity so that it grows out of the given view, unless the view has left the screen meanwhile. */
    void startFrom(View source, Intent intent, CharSequence label) {
        if (!source.isAttachedToWindow()) {
            start(intent, label);
            return;
        }
        Bundle zoomOutOfSource = ActivityOptions
                .makeScaleUpAnimation(source, 0, 0, source.getWidth(), source.getHeight())
                .toBundle();
        startWithOptions(intent, zoomOutOfSource, label);
    }

    private void startWithOptions(Intent intent, Bundle options, CharSequence label) {
        try {
            context.startActivity(intent, options);
        } catch (ActivityNotFoundException | SecurityException e) {
            String message = context.getString(R.string.launch_failed, label);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
