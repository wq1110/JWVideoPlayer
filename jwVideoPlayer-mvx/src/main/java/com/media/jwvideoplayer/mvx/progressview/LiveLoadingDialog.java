package com.media.jwvideoplayer.mvx.progressview;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.media.jwvideoplayer.mvx.R;

/**
 * 直播加载框
 */
public class LiveLoadingDialog extends Dialog {
    private TextView tv_loading;
    private ProgressBar pb_loading;

    public LiveLoadingDialog(Context paramContext) {
        this(paramContext, R.style.Exitdialog);
    }

    public LiveLoadingDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        View loadingView = LayoutInflater.from(context).inflate(
                R.layout.live_loading_dialog, null);
        tv_loading = (TextView) loadingView
                .findViewById(R.id.live_loading_tv);
        pb_loading = loadingView.findViewById(R.id.live_loading_pb);
        setCancelable(true);
        setContentView(loadingView);
    }

    public void setPbDrawableId(int id) {
        try {
            if (id > 0) {
                this.pb_loading.setIndeterminateDrawable(ContextCompat.getDrawable(getContext(), id));
            }
        } catch (Exception e) {

        }
    }

    public void setLoadingMsg(String paramString) {
        if (TextUtils.isEmpty(paramString)) {
            this.tv_loading.setVisibility(View.GONE);
        } else {
            this.tv_loading.setVisibility(View.VISIBLE);
        }
        this.tv_loading.setText(paramString);
    }
}