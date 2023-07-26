package com.media.jwvideoplayer.player.ui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.player.VodMediaConstant;
import com.media.jwvideoplayer.player.listener.ConnectionChangeListener;
import com.media.jwvideoplayer.player.receiver.VodMediaReceiver;
/**
 * Created by Joyce.wang on 2023/3/20.
 * 处理全屏和小屏幕逻辑
 */
public abstract class BaseVideoPlayer extends VideoControlView {
    private static Logger logger = LoggerFactory.getLogger(BaseVideoPlayer.class.getName());

    protected ConnectionChangeListener.ConnectionType currentConnectionType;
    protected VodMediaReceiver vodMediaReceiver;


    public BaseVideoPlayer(@NonNull Context context) {
        super(context);
    }

    public BaseVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BaseVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onHandleMessage(Message msg) {
        super.onHandleMessage(msg);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        initListener();
        registerReceiver();
    }

    private void initListener() {
        vodMediaReceiver = new VodMediaReceiver(new ConnectionChangeListener() {
            @Override
            public void onLocaleChange() {
                if (onEventCallback != null) onEventCallback.onLocaleChange();
            }

            @Override
            public void onConnectionChange(ConnectionType type) {
                currentConnectionType = type;

                logger.i("currentConnectionType:%s", currentConnectionType.name());

                if (ConnectionType.NONE == type && !isCacheFinishFilm) {
                    enableProgressSlide(false);
                } else {
                    enableProgressSlide(true);
                }

                if (ConnectionType.MOBILE == type && (!VodMediaConstant.isMobileTraffic) && null != onEventCallback && isPlaying()) {
                    onVideoPause();
                    onEventCallback.onNetworkChange(new Action<Boolean>() {
                        @Override
                        public void call(Boolean b) {
                            if (b) {
                                onVideoResume();
                            }
                        }
                    });
                } else if (null != onEventCallback) {
                    onEventCallback.onNetworkChange(type);
                }
            }
        });
    }

    private boolean mReceiverTag = false;   //广播接受者标识
    private void registerReceiver() {
        if (!mReceiverTag) {
            mReceiverTag = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            mContext.registerReceiver(vodMediaReceiver, filter);
        }
    }

    private void unRegisterReceiver() {
        if (mReceiverTag) {
            mReceiverTag = false;
            mContext.unregisterReceiver(vodMediaReceiver);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterReceiver();
    }

    public interface Action<T> {
        void call(T t);
    }
}
