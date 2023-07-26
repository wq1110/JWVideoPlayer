package com.media.jwvideoplayer.lib.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.StringRes;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import me.drakeet.support.toast.ToastCompat;


public class ToastUtils {

    private static Toast toast;

    private ToastUtils() {
    }

    /**
     * 显示土司
     */
    public static void showToast(final Context context, String text, final int image) {
        showToastOnMainThread(context, text, image);
    }

    public static void showToast(final Context context, @StringRes int resId, final int image) {
        try {
            showToastOnMainThread(context, context.getResources().getString(resId), image);
        } catch (Exception e) {

        }
    }

    public static void showToast(final Context context, String text) {
        showToastOnMainThread(context, text, -1);
    }

    public static void showToast(final Context context, int textId) {
        if (context == null) {
            return;
        }
        showToastOnMainThread(context, context.getString(textId), -1);
    }

    @SuppressLint("CheckResult")
    private static void showToastOnMainThread(final Context context, String text, final int image) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            showToastUi(context, text, image);
        } else if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToastUi(context, text, image);
                }
            });
        } else {
            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            showToastUi(context, text, image);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {

                        }
                    });
        }
    }

//    /**
//     * 显示土司
//     */
//    private static void showToastUi(final Context context, String text, final int image) {
//        if (context == null || TextUtils.isEmpty(text)) return;
//        try {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
//                if (toast == null) {
//                    toast = ToastCompat.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG);
//                    View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.tv_toast, null);
//                    toast.setView(view);
//                    toast.setDuration(Toast.LENGTH_SHORT);
//                }
//                if (image > 0) {
//                    ((ImageView) toast.getView().findViewById(R.id.iv_smtv_toast)).setBackgroundResource(image);
//                }
//                ((TextView) toast.getView().findViewById(R.id.tv_smtv_toast)).setText(text);
//                toast.show();
//            } else {
//                if (null != toast) {
//                    toast.cancel();
//                    toast = null;
//                }
//                toast = ToastCompat.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG);
//                View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.tv_toast, null);
//                TextView tv_toast = (TextView) view.findViewById(R.id.tv_smtv_toast);
//                ImageView iv_toast = (ImageView) view.findViewById(R.id.iv_smtv_toast);
//                tv_toast.setText(text);
//                if (image > 0) {
//                    iv_toast.setBackgroundResource(image);
//                }
//                toast.setView(view);
//                toast.setDuration(Toast.LENGTH_SHORT);
//                toast.show();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 显示土司
     */
    private static void showToastUi(final Context context, String text, final int image) {
        if (context == null || TextUtils.isEmpty(text)) return;
        if (context instanceof Activity && (((Activity)context).isFinishing() || ((Activity)context).isDestroyed())) return;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                if (toast == null) {
                    toast = ToastCompat.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG);
                }
            } else {
                if (null != toast) {
                    toast.cancel();
                    toast = null;
                }
                toast = ToastCompat.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG);
            }
            toast.setText(text);
            toast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
