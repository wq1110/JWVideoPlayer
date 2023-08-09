package com.media.jwvideoplayer;

import com.media.jwvideoplayer.cache.VideoProxyCacheManager;
import com.media.jwvideoplayer.cache.common.VideoParams;
import com.media.jwvideoplayer.cache.listener.IVideoCacheListener;
import com.media.jwvideoplayer.cache.model.VideoCacheInfo;
import com.media.jwvideoplayer.cache.utils.ProxyCacheUtils;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.player.ui.VideoView;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Joyce.wang on 2023/8/8.
 */
public class LocalProxyVideoControl {
    private static Logger logger = LoggerFactory.getLogger(LocalProxyVideoControl.class.getName());
    private VideoView mVideoView;
    private String mVideoUrl;
    private String mSourceId = "";

    public LocalProxyVideoControl(VideoView videoView) {
        mVideoView = videoView;
    }

    //开始cache任务
    public void startRequestVideoInfo(String sourceId, String videoUrl, Map<String, String> headers, Map<String, Object> extraParams) {
        mVideoUrl = videoUrl;//待请求的url
        mSourceId = sourceId;

        String md5 = ProxyCacheUtils.computeMD5(sourceId);

        VideoProxyCacheManager.getInstance().addCacheListener(sourceId, mVideoUrl, mListener);//添加缓存listener,有开始缓存、缓存进度更新、缓存失败、缓存成功的回调
        VideoProxyCacheManager.getInstance().setPlayingUrlMd5(md5);
        VideoProxyCacheManager.getInstance().startRequestVideoInfo(sourceId, videoUrl, headers, extraParams);
    }

    public void pauseLocalProxyTask() {
        VideoProxyCacheManager.getInstance().pauseCacheTask(mSourceId);
    }

    public void resumeLocalProxyTask() {
        VideoProxyCacheManager.getInstance().resumeCacheTask(mSourceId);
    }

    public void seekToCachePosition(long position) {
        long totalDuration = mVideoView.getDuration();
        if (totalDuration > 0) {
            float percent = position * 1.0f / totalDuration;
            VideoProxyCacheManager.getInstance().seekToCacheTaskFromClient(mSourceId, percent);
        }
    }

    public void releaseLocalProxyResources() {
        VideoProxyCacheManager.getInstance().stopCacheTask(mSourceId);   //停止视频缓存任务
        VideoProxyCacheManager.getInstance().releaseProxyReleases(mSourceId);
    }

    //cache task progress listener
    private IVideoCacheListener mListener = new IVideoCacheListener() {
        @Override
        public void onCacheStart(VideoCacheInfo cacheInfo) {
            logger.d("onCacheStart");
        }

        @Override
        public void onCacheProgress(VideoCacheInfo cacheInfo) {
            Map<String, Object> params = new HashMap<>();
            params.put(VideoParams.PERCENT, cacheInfo.getPercent());
            params.put(VideoParams.CACHE_SIZE, cacheInfo.getCachedSize());
        }

        @Override
        public void onCacheError(VideoCacheInfo cacheInfo, int errorCode) {
            logger.d("onCacheError");
        }

        @Override
        public void onCacheForbidden(VideoCacheInfo cacheInfo) {

        }

        @Override
        public void onCacheFinished(VideoCacheInfo cacheInfo) {
            Map<String, Object> params = new HashMap<>();
            params.put(VideoParams.PERCENT, 100f);
            params.put(VideoParams.TOTAL_SIZE, cacheInfo.getTotalSize());
        }
    };
}