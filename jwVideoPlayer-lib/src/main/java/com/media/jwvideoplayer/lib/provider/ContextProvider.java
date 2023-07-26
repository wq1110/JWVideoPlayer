package com.media.jwvideoplayer.lib.provider;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * Created by Joyce.wang on 2022/10/11.
 * 全局Context提供者
 */
public class ContextProvider {
    @SuppressLint("StaticFieldLeak")
    private static volatile ContextProvider instance;
    private Context mContext;

    private ContextProvider(Context context) {
        mContext = context;
    }

    /**
     * 获取实例
     */
    private static ContextProvider get() {
        if (instance == null) {
            synchronized (ContextProvider.class) {
                if (instance == null) {
                    Context context = ACProvider.mContext;
                    if (context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    instance = new ContextProvider(context);
                }
            }
        }
        return instance;
    }

    /**
     * 获取上下文
     */
    public static Context getContext() {
        return ContextProvider.get().mContext;
    }

    public static Application getApplication() {
        return (Application) ContextProvider.get().mContext.getApplicationContext();
    }
}