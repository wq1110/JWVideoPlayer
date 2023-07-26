package com.media.jwvideoplayer.mvx.appstat;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.media.jwvideoplayer.mvx.appstat.features.IEventsFeatures;
import com.media.jwvideoplayer.mvx.rxjava.StreamController;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

class EventsFeatureImpl implements IEventsFeatures, Application.ActivityLifecycleCallbacks{
    StreamController<KeyEvent> scUserKeyEvent = new StreamController<>();               //按键
    StreamController<MotionEvent> scUserTouchEvent = new StreamController<>();    //触控 鼠标点击
    StreamController<MotionEvent> scUserGenericEvent = new StreamController<>();  //鼠标
    StreamController<MotionEvent> scUserTrackballEvent = new StreamController<>(); //轨迹球

    public EventsFeatureImpl(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        activity.getWindow().setCallback(new CallBackWrapper(activity.getWindow().getCallback()));
    }
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if(activity.getWindow().getCallback() instanceof CallBackWrapper) {
            activity.getWindow().setCallback(((CallBackWrapper)activity.getWindow().getCallback()).originalCallback);
        }
    }
    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public Observable<Object> userOperationHappening(int duration, TimeUnit timeUnit) {
        return Observable.merge(
                scUserTouchEvent.stream(), scUserKeyEvent.stream(), scUserGenericEvent.stream(), scUserTrackballEvent.stream()
        ).throttleFirst(duration, timeUnit).map(inputEvent -> {
            return "";
        });
    }

    private class CallBackWrapper implements Window.Callback{
        Window.Callback originalCallback;

        public CallBackWrapper(Window.Callback originalCallback) {
            this.originalCallback = originalCallback;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event){
            scUserKeyEvent.push(event);
            return originalCallback.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return originalCallback.dispatchKeyShortcutEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            scUserTouchEvent.push(event);
            return originalCallback.dispatchTouchEvent(event);
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            scUserTrackballEvent.push(event);
            return originalCallback.dispatchTrackballEvent(event);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            scUserGenericEvent.push(event);
            return originalCallback.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return originalCallback.dispatchPopulateAccessibilityEvent(event);
        }

        @Nullable
        @Override
        public View onCreatePanelView(int featureId) {
            return originalCallback.onCreatePanelView(featureId);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
            return originalCallback.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, @Nullable View view, @NonNull Menu menu) {
            return originalCallback.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, @NonNull Menu menu) {
            return originalCallback.onMenuOpened(featureId, menu);
        }

        @Override
        public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
            return originalCallback.onMenuItemSelected(featureId, item);
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            originalCallback.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onContentChanged() {
            originalCallback.onContentChanged();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            originalCallback.onWindowFocusChanged(hasFocus);
        }

        @Override
        public void onAttachedToWindow() {
            originalCallback.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            originalCallback.onDetachedFromWindow();
        }

        @Override
        public void onPanelClosed(int featureId, @NonNull Menu menu) {
            originalCallback.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return originalCallback.onSearchRequested();
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onSearchRequested(SearchEvent searchEvent) {
            return originalCallback.onSearchRequested(searchEvent);
        }

        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            return originalCallback.onWindowStartingActionMode(callback);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
            return originalCallback.onWindowStartingActionMode(callback, type);
        }

        @Override
        public void onActionModeStarted(ActionMode mode) {
            originalCallback.onActionModeStarted(mode);
        }

        @Override
        public void onActionModeFinished(ActionMode mode) {
            originalCallback.onActionModeFinished(mode);
        }
    }
}
