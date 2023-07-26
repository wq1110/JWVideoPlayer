package com.media.jwvideoplayer.mvx.appstat;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.media.jwvideoplayer.mvx.appstat.features.IActivitiesFeatures;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
* 处理Activity范围的事情
* 职责:
*/
class ActivitiesFeatureImpl implements Application.ActivityLifecycleCallbacks, IActivitiesFeatures {
    private final List<Activity> activities = new ArrayList<>();
    private WeakReference<Activity> atyRef;
    private String visibleScene = "default";
    private final List<IActivitiesEventListener> activitiesEventListeners = new ArrayList<>();

    public ActivitiesFeatureImpl(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    private void updateScene(Activity activity) {
        visibleScene = activity.getClass().getName();
        atyRef = new WeakReference<>(activity);
    }

    public String getVisibleScene() {
        return visibleScene;
    }

    public Activity getTopActivity() {
        return atyRef == null? null: atyRef.get();
    }

    public List<Activity> getAllActivity() {
        return new ArrayList<>(activities);
    }

    public void clearAllActivity() {
        for (Activity activity : activities) {
            activity.finish();
        }
        visibleScene = null;
        atyRef = null;
        activities.clear();
    }

    @Override
    public void addActivitiesEventListener(IActivitiesEventListener listener) {
        activitiesEventListeners.add(listener);
    }

    @Override
    public void removeActivitiesEventListener(IActivitiesEventListener listener) {
        activitiesEventListeners.remove(listener);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        activities.add(activity);
        for (IActivitiesEventListener listener : activitiesEventListeners) {
            listener.onNewActivityIn(activity.getComponentName(), activity.getCallingActivity());
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        updateScene(activity);
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
    public void onActivityDestroyed(@NonNull Activity activity) {
        activities.remove(activity);
    }
}
