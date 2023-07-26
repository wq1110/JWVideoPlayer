package com.media.jwvideoplayer.mvx.appstat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.lib.provider.ContextProvider;
import com.media.jwvideoplayer.lib.utils.ProcessUtils;
import com.media.jwvideoplayer.mvx.appstat.features.IAppFeatures;
import com.media.jwvideoplayer.mvx.async.ThreadsBox;
import com.media.jwvideoplayer.mvx.rxjava.StreamController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;

public class AppFeatureImpl implements IAppFeatures, Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    private StreamController<AppStat> scAppStat = new StreamController<>();
    private static long appStartTimeStamp = System.currentTimeMillis();
    private int resumeActivityCount = 0;
    private AppStat currentStat = AppStat.Foreground;
    HandlerThread handlerThread;
    Handler handler;
    TopCmd topCmd;

    public AppFeatureImpl(Application application) {
        application.registerActivityLifecycleCallbacks(this);
        application.registerComponentCallbacks(this);
        handlerThread = ThreadsBox.getNewHandlerThread(AppFeatureImpl.class.getSimpleName());
        handler = new Handler(handlerThread.getLooper(), null);
        topCmd = new TopCmd(handler);
        topCmd.start();
    }

    @Override
    public long getAppRunningTime() {
        return System.currentTimeMillis() - appStartTimeStamp;
    }

    @Override
    public AppStat getCurrentAppStat() {
        return currentStat;
    }

    @Override
    public boolean isAppForeground() {
        return currentStat == AppStat.Foreground;
    }

    @Override
    public boolean isAppBackground() {
        return currentStat == AppStat.Background;
    }

    @Override
    public Observable<AppStat> appStatChanged() {
        return scAppStat.stream();
    }

    @Override
    public void exitApp() {
        AppManager.INSTANCE.getActivities().clearAllActivity();
        onDispatchBackground();
        Process.killProcess(Process.myPid());
    }

    @Override
    public String collectionAppStatusSnapShot() {
        int pid = Process.myPid();
        StringBuilder builder = new StringBuilder();
        ActivityManager activityManager = ((ActivityManager) ContextProvider.getContext().getSystemService(Context.ACTIVITY_SERVICE));
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        Set<Thread> set = Thread.getAllStackTraces().keySet();

        builder.append("System:\n")
                .append(ProcessUtils.exeCommandsWithFormat("cat /proc/meminfo\n"))
                .append("\nApp:\n")
                .append("\nJava Threads:").append(set.size()).append("\n")
                .append(ProcessUtils.exeCommandsWithFormat("cat /proc/" + pid + "/status | grep Threads")).append("\n")
                .append("\nApp Pss: ").append(Debug.getPss() / 1024).append(" mb\n")
                .append("Android System memoryInfo ").append("availMem: ").append(memoryInfo.availMem / 1024 / 1024).append("mb").append(" totalMem: ").append(memoryInfo.totalMem / 1024 / 1024).append("mb").append(" threshold:").append(memoryInfo.threshold / 1024 / 1024).append("mb\n")
                .append("\nJavaHeap total: ").append(Runtime.getRuntime().totalMemory() / 1024 / 1024).append("mb").append(" used: ").append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024).append("mb\n")
                .append("Android System JavaHeap limit: ").append(activityManager.getMemoryClass()).append("mb\n")
                .append("\nThreads:\n");
        for (Thread thread : set) {
            builder.append(thread.getName());
            builder.append("\t");
            builder.append(thread.getState());
            builder.append("\n");
        }
        return builder.toString();
    }

    @Override
    public List<SysStat> getSysStatus() {
        ArrayList<SysStat> sysStats = new ArrayList<>();
        if (topCmd.isIOBusy) sysStats.add(SysStat.IO_BUSY);
        if (topCmd.isLowMem) sysStats.add(SysStat.LOW_MEMORY);
        return sysStats;
    }

    private void onDispatchForeground() {
        currentStat = AppStat.Foreground;
        scAppStat.push(currentStat);
    }

    private void onDispatchBackground() {
        currentStat = AppStat.Background;
        scAppStat.push(currentStat);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (resumeActivityCount++ == 0) {
            onDispatchForeground();
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (--resumeActivityCount == 0) {
            onDispatchBackground();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN && currentStat == AppStat.Foreground) { // fallback
            onDispatchBackground();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }

    static class TopCmd implements Runnable {
        Handler handler;
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        final static long Duration = 1000 * 1;
         long Ob_Duration_Sec = 3;
        String argForDisplayThread;
        String argForDisplayMax;
        String argForDisplayTimes;
        volatile boolean isIOBusy = false;
        volatile boolean isLowMem = false;

        public TopCmd(Handler handler) {
            this.handler = handler;
        }

        int exceptionTimes = 0;

        boolean initArg() {
            try {
                String top = ProcessUtils.exeCommandsWithFormat("top -h");
                if (!top.toLowerCase().startsWith("usage")) {
                    top = ProcessUtils.exeCommandsWithFormat("top --help");
                }
                String[] h = top.split("\n");
                int index;
                for (String str : h) {
                    if (str.contains("threads") && (index = str.indexOf("-")) >= 0) {
                        argForDisplayThread = str.substring(index, index + 2);
                    } else if (str.contains("Maximum") && (index = str.indexOf("-")) >= 0) {
                        argForDisplayMax = str.substring(index, index + 2);
                    } else if (str.toLowerCase().contains("exit") && (index = str.indexOf("-")) >= 0) {
                        argForDisplayTimes = str.substring(index, index + 2);
                    }
                }
            } catch (Exception e) {
                exceptionTimes++;
            }
            return exceptionTimes >= 3;
        }

        void start() {
            handler.postDelayed(this, TopCmd.Duration);
        }

        void startInternal(long Duration) {
            handler.postDelayed(this, Duration);
        }

        @Override
        public void run() {
            if (argForDisplayTimes != null && argForDisplayThread != null && argForDisplayMax != null) {
                String top = ProcessUtils.exeCommandsWithFormat("top " + argForDisplayThread + " " + argForDisplayMax + " 5 " + argForDisplayTimes + " 1");
                String[] contents = top.split("\n");
                boolean isLowMem = false;
                boolean isIOBusy = false;
                for (String str : contents) {
                    if (str.contains(" kswapd")) {
                        //linux mem
                        isLowMem = true;
                    } else if (str.contains(" mmcqd")  //B9 E9....
                            || str.contains(" nand")     //MXQ:dolphin_fvd_p1, RedOne...
                    ) {
                        //linux io
                        isIOBusy = true;
                    }
                }
                this.isIOBusy = isIOBusy;
                this.isLowMem = isLowMem;
                if (isIOBusy || isLowMem) {
                    Ob_Duration_Sec += 2;
                    startInternal(Ob_Duration_Sec);
                    return;
                }
                Ob_Duration_Sec = 3;
            } else if (initArg()) {
                return;
            }
            start();
        }
    }
}
