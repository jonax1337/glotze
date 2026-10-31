package local.tvhome;

import android.content.Context;
import android.service.dreams.DreamService;
import android.view.LayoutInflater;
import android.view.View;

/** The launcher's own screen saver, replacing the system's. Any key wakes the TV. */
public final class LockScreenDream extends DreamService {
    private LockScreen lockScreen;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        Context dressed = Design.dressed(this);
        View screen = LayoutInflater.from(dressed).inflate(R.layout.lock_screen, null);
        setContentView(screen);
        lockScreen = new LockScreen(screen, dressed);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        lockScreen.start();
    }

    @Override
    public void onDreamingStopped() {
        lockScreen.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        lockScreen.release();
        super.onDetachedFromWindow();
    }
}
