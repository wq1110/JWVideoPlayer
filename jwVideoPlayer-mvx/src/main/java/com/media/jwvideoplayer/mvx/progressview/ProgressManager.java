package com.media.jwvideoplayer.mvx.progressview;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.DrawableRes;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressManager {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private final static AtomicInteger showingProgressCnt = new AtomicInteger(0);
    private static LiveLoadingDialog loadingDialog;
    private final static OpShow opShow = new OpShow();
    private final static OpDismiss opDimiss = new OpDismiss();
    private static int loadingDialogDrawableId;
    private static int loadingDialogTheme;
    /*private static MFCLoadingView mfcLoadingView;

    static {
        mfcLoadingView = new MFCLoadingView(MyApplication.getInstance());
    }*/

    private ProgressManager() {

    }

    public static void initLoadingDialogStyle(int dialogTheme, @DrawableRes int progressDrawable) {
        loadingDialogTheme = dialogTheme;
        loadingDialogDrawableId = progressDrawable;
    }

    public static void showProgressDialog(Context context) {
        showProgressDialog(context, "");
    }

    public static void showProgressDialog(Context context, int strResId) {
        String message = context.getString(strResId);
        showProgressDialog(context, message);
    }

    public static void showProgressDialog(Context context, String message) {
        if (!(context instanceof Activity)) {
            return;
        }

        opShow.setMsg(message);
        opShow.setContext(context);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            opShow.run();
        } else {
            handler.post(opShow);
        }
    }

    static class OpShow implements Runnable {
        WeakReference<Context> contextRef;
        String mMsg;

        public void setContext(Context context) {
            contextRef = null;
            if (context != null) {
                this.contextRef = new WeakReference<Context>(context);
            }
        }

        public void setMsg(String msg) {
            this.mMsg = msg;
        }

        @Override
        public void run() {
            if (contextRef == null || contextRef.get() == null || mMsg == null) {
                return;
            }
            Context context = contextRef.get();
            String message = mMsg;
            try {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.cancel();
                }
                if (loadingDialogTheme <= 0) {
                    loadingDialog = new LiveLoadingDialog(context);
                } else {
                    loadingDialog = new LiveLoadingDialog(context, loadingDialogTheme);
                }
                loadingDialog.setLoadingMsg(message);
                loadingDialog.setPbDrawableId(loadingDialogDrawableId);
                loadingDialog.show();
                loadingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialog = null;
                    }
                });
            } catch (Throwable e) {
                Log.w(ProgressManager.class.getSimpleName(), "ProgressDialog OpShow", e);
            }
        }
    }

    static class OpDismiss implements Runnable {
        @Override
        public void run() {
            try {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.cancel();
                    loadingDialog = null;
                }
            } catch (Throwable e) {
                Log.e(ProgressManager.class.getSimpleName(), "ProgressDialog OpDismiss", e);
            }
        }
    }


    public static void closeProgressDialog(Context context) {
        closeProgressDialog(false);
    }

    public static void closeProgressDialog() {
        closeProgressDialog(false);
    }

    public static void closeProgressDialog(boolean force) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            opDimiss.run();
        } else {
            handler.post(opDimiss);
        }
    }
}
