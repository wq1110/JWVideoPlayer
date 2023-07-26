package com.media.jwvideoplayer.player;

/**
 * Created by Joyce.wang on 2022/4/20.
 */
public class PlayerHelper {
    private static final String TAG = PlayerHelper.class.getSimpleName();
    public static final int PV_PLAYER__IjkExoMediaPlayer = 0;
    public static final int PV_PLAYER__AndroidMediaPlayer = 1;
    public static final int PV_PLAYER__IjkMediaPlayer = 2;

    private PlayerPickerManager mPlayerPickerManager;
    private int currentPlayerType = PV_PLAYER__IjkMediaPlayer;
    private String currentPlayerTypeName = getPlayerTypeName(currentPlayerType);

    private volatile static PlayerHelper instance;

    private PlayerHelper() {
    }

    public static PlayerHelper getInstance() {
        if (instance == null) {
            synchronized(PlayerHelper.class) {
                if (instance == null) {
                    instance = new PlayerHelper();
                }
            }
        }
        return instance;
    }

    public void setCurrentPlayerType(int playerType) {
        currentPlayerType = playerType;
        currentPlayerTypeName = getPlayerTypeName(playerType);
    }

    public int getCurrentPlayerType() {
        return currentPlayerType;
    }

    public String getCurrentPlayerTypeName() {
        return currentPlayerTypeName;
    }

    public static String getPlayerTypeName(int playerType) {
        switch (playerType) {
            case PV_PLAYER__AndroidMediaPlayer:
                return "AndroidMediaPlayer";
            case PV_PLAYER__IjkExoMediaPlayer:
                return "IjkExoPlayer";
            case PV_PLAYER__IjkMediaPlayer:
                return "IjkMediaPlayer";
            default:
                return "Unknown";
        }
    }

    public void setPlayerPickerManager(PlayerPickerManager playerPickerManager) {
        mPlayerPickerManager = playerPickerManager;
    }

    public int getPlayerTypeByFormat(String format, boolean isTrailer) {
        if (isTrailer) {
            return PV_PLAYER__IjkExoMediaPlayer;
        }

        if(null != mPlayerPickerManager) {
            int playerType = mPlayerPickerManager.getPlayerType(format, currentPlayerType);
            setCurrentPlayerType(playerType);
        }
        return currentPlayerType;
    }
}
