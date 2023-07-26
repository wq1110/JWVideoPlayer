package com.media.jwvideoplayer.lib.callback;

/**
 * Created by Joyce.wang on 2023/6/28.
 */
import android.os.Handler;
import android.os.Looper;

//通用单数据回调
public abstract class ICallBack<T> {
    ICallBack<T> base;
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    //执行体
    public abstract void call(T t);

    public ICallBack() {
    }

    private ICallBack(ICallBack<T> base) {
        this.base = base;
    }

    //一定在UI线程中执行
    public ICallBack<T> inUIThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) return this;
        return new ICallBack<T>(this) {
            @Override
            public void call(final T t) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        base.call(t);
                        base = null;
                    }
                });
            }
        };
    }
}