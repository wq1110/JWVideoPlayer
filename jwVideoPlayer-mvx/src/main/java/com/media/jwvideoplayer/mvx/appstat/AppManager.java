package com.media.jwvideoplayer.mvx.appstat;

import android.app.Application;

import com.media.jwvideoplayer.mvx.appstat.features.IActivitiesFeatures;
import com.media.jwvideoplayer.mvx.appstat.features.IAppFeatures;
import com.media.jwvideoplayer.mvx.appstat.features.IEventsFeatures;

/**
 * *应用 运行状态 管理类
@Author Damon
*/
public enum AppManager {
    INSTANCE;

    private ActivitiesFeatureImpl mActivitiesImpl;
    private AppFeatureImpl mAppImpl;
    private EventsFeatureImpl mEventsImpl;

    public synchronized void init(Application application) {
        if (mActivitiesImpl == null) mActivitiesImpl = new ActivitiesFeatureImpl(application);
        if (mAppImpl == null)  mAppImpl = new AppFeatureImpl(application);
        if (mEventsImpl == null)  mEventsImpl = new EventsFeatureImpl(application);
    }


    public IActivitiesFeatures getActivities() {
        return mActivitiesImpl;
    }

    public IAppFeatures getApp() {
        return mAppImpl;
    }

    public IEventsFeatures getEvents() {
        return mEventsImpl;
    }


//    public static String getTopActivityName() {
//        long start = System.currentTimeMillis();
//        try {
//            Class activityThreadClass = Class.forName("android.app.ActivityThread");
//            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
//            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
//            activitiesField.setAccessible(true);
//
//            Map<Object, Object> activities;
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
//                activities = (HashMap<Object, Object>) activitiesField.get(activityThread);
//            } else {
//                activities = (ArrayMap<Object, Object>) activitiesField.get(activityThread);
//            }
//            if (activities.size() < 1) {
//                return null;
//            }
//            for (Object activityRecord : activities.values()) {
//                Class activityRecordClass = activityRecord.getClass();
//                Field pausedField = activityRecordClass.getDeclaredField("paused");
//                pausedField.setAccessible(true);
//                if (!pausedField.getBoolean(activityRecord)) {
//                    Field activityField = activityRecordClass.getDeclaredField("activity");
//                    activityField.setAccessible(true);
//                    Activity activity = (Activity) activityField.get(activityRecord);
//                    return activity.getClass().getName();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            long cost = System.currentTimeMillis() - start;
//        }
//        return null;
//    }

}