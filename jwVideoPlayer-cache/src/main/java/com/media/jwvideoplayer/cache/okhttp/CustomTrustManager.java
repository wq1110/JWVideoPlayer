package com.media.jwvideoplayer.cache.okhttp;

import android.annotation.SuppressLint;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
/**
 * Created by Joyce.wang on 2023/4/5.
 */
public class CustomTrustManager implements X509TrustManager {

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
