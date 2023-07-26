package com.media.jwvideoplayer.mvx.layout;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.lib.callback.ICallBack;
import com.media.jwvideoplayer.lib.provider.ContextProvider;
import com.media.jwvideoplayer.mvx.async.ThreadsBox;

import java.util.concurrent.CyclicBarrier;

/**
 * 异步解析 布局xml
 * 职责: 线程IO 主线程createView
 */
public class LayoutInflaterEx {
    private static Handler mHandler = new Handler(ThreadsBox.getNewHandlerThread("LayoutInflaterEx").getLooper());
    private static LayoutInflater layoutInflaterImpl;

    public LayoutInflaterEx() {

    }

    public static void inflate(Context context, @LayoutRes final int layoutID, final ICallBack<View> callBackSuccess, final ICallBack<Throwable> callBackError, Boolean isAsync) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread())
            throw new RuntimeException("inflate() only can be invoked in UIThread");

        if (layoutInflaterImpl == null) {
            layoutInflaterImpl = LayoutInflater.from(ContextProvider.getContext()).cloneInContext(ContextProvider.getContext());
            layoutInflaterImpl.setFactory(new LayoutInflater.Factory() {
                @Nullable
                @Override
                public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
                    if (Thread.currentThread() != mHandler.getLooper().getThread())
                        return null;
                    CreateViewTask createViewTask = new CreateViewTask(name, layoutInflaterImpl, attrs);
                    createViewTask.run();
                    //ThreadsBox.getDefaultMainHandler().post(createViewTask);
                    try {
                        //Log.v("ttt", "onCreateView 线程:" + Thread.currentThread());
                        // cyclicBarrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return createViewTask.getView();
                }
            });
        }

        if (isAsync) {
            mHandler.post(new AsyncLayoutInflateTask(layoutID, context, callBackSuccess, callBackError));
        } else {
            try {
                if (context != null && callBackSuccess != null) {
                    callBackSuccess.call(LayoutInflater.from(context).inflate(layoutID, context instanceof Activity ? (ViewGroup) ((Activity) context).getWindow().getDecorView() : null, false));
                }
            } catch (Exception e) {
                if (callBackError != null) callBackError.call(e);
            }
        }
    }

    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

    static class AsyncLayoutInflateTask implements Runnable {
        int layoutID;
        Context context;
        ICallBack<View> callBackSuccess;
        ICallBack<Throwable> callBackError;

        public AsyncLayoutInflateTask(int layoutID, Context context, ICallBack<View> callBackSuccess, ICallBack<Throwable> callBackError) {
            this.layoutID = layoutID;
            this.context = context;
            this.callBackSuccess = callBackSuccess;
            this.callBackError = callBackError;
        }

        @Override
        public void run() {
            try {
                if (context != null && callBackSuccess != null) {
                    callBackSuccess.inUIThread().call(layoutInflaterImpl.inflate(layoutID, null));
                }
            } catch (Exception e) {
                if (callBackError != null) callBackError.inUIThread().call(e);
            }
        }
    }

    static class CreateViewTask implements Runnable {
        String name;
        LayoutInflater layoutInflater;
        AttributeSet attrs;
        View view;

        private static final String[] sClassPrefixList = {
                "android.widget.",
                "android.webkit.",
                "android.app.",
                "android.view.",
        };

        public CreateViewTask(String name, LayoutInflater layoutInflater, AttributeSet attrs) {
            this.name = name;
            this.layoutInflater = layoutInflater;
            this.attrs = attrs;
        }

        @Override
        public void run() {
            if (-1 == name.indexOf('.')) {
                for (String prefix : sClassPrefixList) {
                    try {
                        view = layoutInflater.createView(name, prefix, attrs);
                        if (view != null) break;
                    } catch (Exception ignore) {
                    }
                }
            } else {
                try {
                    view = layoutInflater.createView(name, null, attrs);
                } catch (Exception ignore) {
                }
            }
            try {
                //Log.v("ttt", "CreateView 线程:" + Thread.currentThread());
                //cyclicBarrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public View getView() {
            return view;
        }
    }
}
