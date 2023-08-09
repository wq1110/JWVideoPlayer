package com.media.jwvideoplayer.cache.okhttp;

import androidx.annotation.NonNull;

import com.media.jwvideoplayer.cache.common.VideoCacheException;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Joyce.wang on 2023/4/5.
 * okhttp 请求的管理类
 */
public class OkHttpManager {
    private static Logger logger = LoggerFactory.getLogger(OkHttpManager.class.getName());
    private static OkHttpManager sInstance = null;

    private NetworkConfig mConfig;
    private IHttpPipelineListener mListener;
    private Map<String, OkHttpControl> mHttpControlMap = new ConcurrentHashMap<>();

    public static OkHttpManager getInstance() {
        if (sInstance == null) {
            synchronized (OkHttpManager.class) {
                if (sInstance == null) {
                    sInstance = new OkHttpManager();
                }
            }
        }
        return sInstance;
    }

    //初始化的时候设置
    public void initConfig(@NonNull NetworkConfig config, @NonNull IHttpPipelineListener listener) {
        mConfig = config;
        mListener = listener;
    }

    public OkHttpControl createOkHttpControl(String url, Map<String, String> headers, boolean isHeadRequest) throws VideoCacheException {
        OkHttpControl control = new OkHttpControl(url, headers, isHeadRequest, mListener, mConfig);
        try {
            control.markRequest();
        } catch (Exception e) {
            logger.w("createOkHttpControl make request failed, exception = " + e.getMessage());
            throw new VideoCacheException(e);
        }
        return control;
    }

    public String getFinalUrl(String url, Map<String, String> headers) throws VideoCacheException {
        if (mHttpControlMap.containsKey(url)) {
            OkHttpControl control = mHttpControlMap.get(url);
            if (control != null) {
                return control.getFinalUrl();
            } else {
                control = createOkHttpControl(url, headers, true);
                mHttpControlMap.put(url, control);
                return control.getFinalUrl();
            }
        } else {
            OkHttpControl control = createOkHttpControl(url, headers, true);
            mHttpControlMap.put(url, control);
            return control.getFinalUrl();
        }
    }

    public int getRedirectCount(String url) {
        if (mHttpControlMap.containsKey(url)) {
            return mHttpControlMap.get(url).getRedirectCount();
        }
        return 0;
    }

    public String getContentType(String url, Map<String, String> headers) throws VideoCacheException {
        if (mHttpControlMap.containsKey(url)) {
            OkHttpControl control = mHttpControlMap.get(url);
            if (control != null) {
                return control.getContentType();
            } else {
                control = createOkHttpControl(url, headers, true);
                mHttpControlMap.put(url, control);
                return control.getContentType();
            }
        } else {
            OkHttpControl control = createOkHttpControl(url, headers, true);
            mHttpControlMap.put(url, control);
            return control.getContentType();
        }
    }

    public long getContentLength(String url, Map<String, String> headers) throws VideoCacheException {
        if (mHttpControlMap.containsKey(url)) {
            OkHttpControl control = mHttpControlMap.get(url);
            if (control != null) {
                return control.getContentLength();
            } else {
                control = createOkHttpControl(url, headers, true);
                mHttpControlMap.put(url, control);
                return control.getContentLength();
            }
        } else {
            OkHttpControl control = createOkHttpControl(url, headers, true);
            mHttpControlMap.put(url, control);
            return control.getContentLength();
        }
    }

    public InputStream getResponseBody(String url, Map<String, String> headers, @NonNull IFetchResponseListener listener) throws VideoCacheException {
        OkHttpControl control = createOkHttpControl(url, headers, false);
        mHttpControlMap.put(url, control);

        //一条请求获取body和contentLength两个数据
        listener.onContentLength(control.parseContentLengthFromContentRange());
        return control.getResponseBody();
    }

    public void remove(String key) {
        mHttpControlMap.remove(key);
    }
}
