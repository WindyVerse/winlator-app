package com.winlator.core;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AppUtils {
    public static void keepScreenOn(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void showKeyboard(AppCompatActivity activity) {
        final InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            activity.getWindow().getDecorView().postDelayed(() -> imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0), 500L);
        }
        else imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void hideSystemUI(final Activity activity) {
        Window window = activity.getWindow();
        final View decorView = window.getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            final WindowInsetsController insetsController = decorView.getWindowInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else {
            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            decorView.setSystemUiVisibility(flags);
            decorView.setOnSystemUiVisibilityChangeListener((visibility) -> {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) decorView.setSystemUiVisibility(flags);
            });
        }
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static void runDelayed(final Runnable callback, long delay) {
        if (callback == null) return;
        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                callback.run();
            }
        }, delay);
    }
}
