package com.media.jwvideoplayer.mvx.appstat.features;


import java.util.List;

import io.reactivex.Observable;

/**
* 应用 层面
* 职责:
* @author Damon
*/
public interface IAppFeatures {
    long getAppRunningTime();

    AppStat getCurrentAppStat();

    boolean isAppForeground();

    boolean isAppBackground();

    Observable<AppStat> appStatChanged();

    void exitApp();

    String collectionAppStatusSnapShot();

    List<SysStat> getSysStatus();

    enum SysStat {
        LOW_MEMORY,
        IO_BUSY
    }

     enum AppStat {
        Background,
        Foreground
    }

    enum AppMemoryWaring {

    }
}
