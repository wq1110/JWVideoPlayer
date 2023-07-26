package com.media.jwvideoplayer.player.base.model;

import java.io.File;
import java.util.Map;

/**
 * Created by Joyce.wang on 2023/3/15.
 * video 内部接收数据
 */
public class VideoModel {
    String mUrl;

    File mCachePath;

    Map<String, String> mHeaders;

    float speed = 1;

    boolean looping;

    boolean isCache;

    String overrideExtension;

    public VideoModel(String url, Map<String, String> headers, boolean loop, float speed, boolean isCache, File cachePath, String overrideExtension) {
        this.mUrl = url;
        this.mHeaders = headers;
        this.looping = loop;
        this.speed = speed;
        this.isCache = isCache;
        this.mCachePath = cachePath;
        this.overrideExtension = overrideExtension;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public void setHeaders(Map<String, String> headers) {
        this.mHeaders = headers;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean cache) {
        isCache = cache;
    }

    public File getCachePath() {
        return mCachePath;
    }

    public void setCachePath(File cachePath) {
        this.mCachePath = cachePath;
    }

    public String getOverrideExtension() {
        return overrideExtension;
    }

    public void setOverrideExtension(String overrideExtension) {
        this.overrideExtension = overrideExtension;
    }
}
