package com.media.jwvideoplayer.mvx.appstat.features;

import android.app.Activity;
import android.content.ComponentName;

import java.util.List;

/**
*  Activity层面
* 职责:
*/
public interface IActivitiesFeatures {
    Activity getTopActivity();
    List<Activity> getAllActivity();
    void clearAllActivity();
    void addActivitiesEventListener(IActivitiesEventListener listener);
    void removeActivitiesEventListener(IActivitiesEventListener listener);

    interface IActivitiesEventListener {
        void onNewActivityIn(ComponentName newActivity, ComponentName invokeActivity);
    }
}
