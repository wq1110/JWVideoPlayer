package com.media.jwvideoplayer.cache.okhttp;

/**
 * Created by Joyce.wang on 2023/4/5.
 */
public interface IFetchResponseListener {

    //通过一条请求获取contentLength
    void onContentLength(long contentLength);
}