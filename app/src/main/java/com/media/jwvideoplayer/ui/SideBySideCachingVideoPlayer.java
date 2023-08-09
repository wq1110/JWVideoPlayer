package com.media.jwvideoplayer.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.LocalProxyVideoControl;
import com.media.jwvideoplayer.cache.utils.ProxyCacheUtils;
import com.media.jwvideoplayer.player.ui.StandardVideoPlayer;

/**
 * Created by Joyce.wang on 2023/8/8.
 * 边下边播Player
 */
public class SideBySideCachingVideoPlayer extends StandardVideoPlayer {
    private LocalProxyVideoControl mLocalProxyVideoControl;

    public SideBySideCachingVideoPlayer(@NonNull Context context) {
        super(context);
    }

    public SideBySideCachingVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SideBySideCachingVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SideBySideCachingVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mLocalProxyVideoControl = new LocalProxyVideoControl(this);
    }

    @Override
    public void setVideoUrl(String url) {
        String sourceId = url;
        String playUrl = ProxyCacheUtils.getProxyUrl(sourceId, url, null, null);
        mLocalProxyVideoControl.startRequestVideoInfo(sourceId, url, null, null);
        super.setVideoUrl(playUrl);
    }

    @Override
    public void onVideoResume() {
        mLocalProxyVideoControl.resumeLocalProxyTask();
        super.onVideoResume();
    }

    @Override
    public void onVideoPause() {
        mLocalProxyVideoControl.pauseLocalProxyTask();
        super.onVideoPause();
    }

    @Override
    public void seekTo(long position) {
        mLocalProxyVideoControl.seekToCachePosition(position);
        super.seekTo(position);
    }

    @Override
    public void release() {
        mLocalProxyVideoControl.releaseLocalProxyResources();
        super.release();
    }
}
