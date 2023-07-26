package com.media.jwvideoplayer.player.base;

/**
 * Created by Joyce.wang on 2023/3/15.
 * 播放内核工厂
 */
public class PlayerFactory {
    private static Class<? extends IPlayerManager> sPlayerManager;

    public static void setPlayManager(Class<? extends IPlayerManager> playManager) {
        sPlayerManager = playManager;
    }

    public static IPlayerManager getPlayManager() {
        if (sPlayerManager == null) {
            sPlayerManager = IjkPlayerManager.class;
        }
        try {
            return sPlayerManager.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
