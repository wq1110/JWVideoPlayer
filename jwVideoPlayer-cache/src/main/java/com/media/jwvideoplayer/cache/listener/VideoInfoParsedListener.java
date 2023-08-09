package com.media.jwvideoplayer.cache.listener;

import com.media.jwvideoplayer.cache.common.VideoCacheException;
import com.media.jwvideoplayer.cache.m3u8.M3U8;
import com.media.jwvideoplayer.cache.model.VideoCacheInfo;
/**
 * Created by Joyce.wang on 2023/4/14.
 */
public abstract class VideoInfoParsedListener implements IVideoInfoParsedListener {


    @Override
    public void onM3U8ParsedFinished(M3U8 m3u8, VideoCacheInfo cacheInfo) {

    }

    @Override
    public void onM3U8ParsedFailed(VideoCacheException exception, VideoCacheInfo cacheInfo) {

    }

    @Override
    public void onM3U8LiveCallback(VideoCacheInfo cacheInfo) {

    }

    @Override
    public void onNonM3U8ParsedFinished(VideoCacheInfo cacheInfo) {

    }

    @Override
    public void onNonM3U8ParsedFailed(VideoCacheException exception, VideoCacheInfo cacheInfo) {

    }
}