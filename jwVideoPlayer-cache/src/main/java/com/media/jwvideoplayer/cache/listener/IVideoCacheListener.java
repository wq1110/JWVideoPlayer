package com.media.jwvideoplayer.cache.listener;

import com.media.jwvideoplayer.cache.model.VideoCacheInfo;
/**
 * Created by Joyce.wang on 2023/4/14.
 */
public interface IVideoCacheListener {

    void onCacheStart(VideoCacheInfo cacheInfo);

    void onCacheProgress(VideoCacheInfo cacheInfo);

    void onCacheError(VideoCacheInfo cacheInfo, int errorCode);

    void onCacheForbidden(VideoCacheInfo cacheInfo);

    void onCacheFinished(VideoCacheInfo cacheInfo);
}
