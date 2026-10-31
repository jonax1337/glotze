package local.tvhome;

import android.service.dreams.DreamService;

/** The launcher's own screen saver, replacing Fire TV's ambient slideshow. Any key wakes the TV. */
public final class LockScreenDream extends DreamService {
    private LockScreen lockScreen;

    @Override
    public void onCreate() {
        super.onCreate();
        Design.dress(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        setContentView(R.layout.lock_screen);
        lockScreen = new LockScreen(getWindow().getDecorView(), this);
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
