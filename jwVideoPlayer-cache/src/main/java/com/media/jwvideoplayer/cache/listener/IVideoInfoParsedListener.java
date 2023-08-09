package com.media.jwvideoplayer.cache.listener;

import com.media.jwvideoplayer.cache.common.VideoCacheException;
import com.media.jwvideoplayer.cache.m3u8.M3U8;
import com.media.jwvideoplayer.cache.model.VideoCacheInfo;
/**
 * Created by Joyce.wang on 2023/4/14.
 */
public interface IVideoInfoParsedListener {

    //M3U8视频解析成功
    void onM3U8ParsedFinished(M3U8 m3u8, VideoCacheInfo cacheInfo);

    //M3U8视频解析失败
    void onM3U8ParsedFailed(VideoCacheException exception, VideoCacheInfo cacheInfo);

    //M3U8视频是直播
    void onM3U8LiveCallback(VideoCacheInfo cacheInfo);

    //非M3U8视频解析成功
    void onNonM3U8ParsedFinished(VideoCacheInfo cacheInfo);

    //非M3U8视频解析失败
    void onNonM3U8ParsedFailed(VideoCacheException exception, VideoCacheInfo cacheInfo);
}