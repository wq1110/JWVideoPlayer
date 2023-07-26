package com.media.jwvideoplayer.mvx.appstat.features;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

/**
* 各种 按键 触控 等事件 层面
* 职责:
*/
public interface IEventsFeatures {
    Observable<Object> userOperationHappening(int duration, TimeUnit timeUnit);
}
