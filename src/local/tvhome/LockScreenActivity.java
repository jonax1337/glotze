package local.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

/**
 * The screen saver as an activity: a preview from the settings, and the idle screen on Fire OS
 * versions whose Ambient Experience would otherwise take over. Any key closes it.
 */
public final class LockScreenActivity extends Activity {
    private LockScreen lockScreen;
    private final Runnable allowSleep =
            () -> getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    /** Stays lit until Fire OS's own sleep time, so a forgotten TV still switches off. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Design.dress(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.lock_screen);
        lockScreen = new LockScreen(findViewById(android.R.id.content), this);
        ScreenSaver screenSaver = new ScreenSaver(this);
        if (screenSaver.sleepsWhenIdle()) {
            getWindow().getDecorView().postDelayed(allowSleep, screenSaver.awakeMsOnceShown());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        lockScreen.start();
    }

    @Override
    protected void onStop() {
        lockScreen.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        getWindow().getDecorView().removeCallbacks(allowSleep);
        lockScreen.release();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            finish();
        }
        return true;
    }
}
