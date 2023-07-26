package com.media.jwvideoplayer.player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.player.listener.ConnectionChangeListener;

public class VodMediaReceiver extends BroadcastReceiver {
    private static Logger logger = LoggerFactory.getLogger(VodMediaReceiver.class.getName());

    private ConnectionChangeListener listener;
    private ConnectionChangeListener.ConnectionType type = ConnectionChangeListener.ConnectionType.UNKNOWN;

    public VodMediaReceiver(ConnectionChangeListener listener) {
        this.listener = listener;
    }

    public ConnectionChangeListener.ConnectionType getType() {
        return type;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        logger.i("VodMediaReceiver onReceive....");
        if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
            logger.e("system language change");
            if (listener != null) listener.onLocaleChange();
        } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectionChangeListener.ConnectionType newType = ConnectionChangeListener.ConnectionType.UNKNOWN;
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null) {
                /** 没有任何网络 */
                logger.d("no active network");
                newType =  ConnectionChangeListener.ConnectionType.NONE;
            } else {
                if(!networkInfo.isConnected()) {
                    logger.d("no connected network");
                    newType =  ConnectionChangeListener.ConnectionType.NONE;
                } else {
                    logger.d("active network is:%d state:%b:%b", networkInfo.getType(), networkInfo.isConnectedOrConnecting(), networkInfo.isConnected());
                    if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                        /** 以太网网络 */
                        newType = ConnectionChangeListener.ConnectionType.ETHERNET;
                    } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        /** wifi网络，当激活时，默认情况下，所有的数据流量将使用此连接 */
                        newType = ConnectionChangeListener.ConnectionType.WIFI;
                    } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        /** mobile网络 */
                        newType = ConnectionChangeListener.ConnectionType.MOBILE;
                    }
                }
            }

            if(null != listener && newType != type) {
                type = newType;
                listener.onConnectionChange(type);
            }
        }
    }
}
