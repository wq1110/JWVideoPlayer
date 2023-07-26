package com.media.jwvideoplayer.player.listener;

public interface ConnectionChangeListener {
    public void onLocaleChange();

    public void onConnectionChange(ConnectionType type);

    public enum ConnectionType {
        WIFI, MOBILE, ETHERNET, NONE, UNKNOWN
    }
}

