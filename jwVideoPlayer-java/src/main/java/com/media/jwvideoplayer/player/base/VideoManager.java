package com.media.jwvideoplayer.player.base;

/**
 * Created by Joyce.wang on 2023/3/15.
 * video manager
 */
public class VideoManager extends BaseVideoManager {
    private static final String TAG = VideoManager.class.getSimpleName();
    private static volatile VideoManager videoManager;

    public static VideoManager getInstance() {
        if (videoManager == null) {
            synchronized (VideoManager.class) {
                if (videoManager == null) {
                    videoManager = new VideoManager();
                }
            }
        }
        return videoManager;
    }

    private VideoManager() {
        init();
    }

    /**
     * 页面销毁了记得调用是否所有的video
     */
    public static void releaseAllVideos() {
        if (VideoManager.getInstance().getMediaPlayerListener() != null) {
            VideoManager.getInstance().getMediaPlayerListener().onCompletion();
        }
        VideoManager.getInstance().releaseMediaPlayer();
    }


    /**
     * 暂停播放
     */
    public static void onPause() {
        if (VideoManager.getInstance().getMediaPlayerListener() != null) {
            VideoManager.getInstance().getMediaPlayerListener().onVideoPause();
        }
    }

    /**
     * 恢复播放
     */
    public static void onResume() {
        if (VideoManager.getInstance().getMediaPlayerListener() != null) {
            VideoManager.getInstance().getMediaPlayerListener().onVideoResume();
        }
    }


    /**
     * 恢复暂停状态
     *
     * @param seek 是否产生seek动作,直播设置为false
     */
    public static void onResume(boolean seek) {
        if (VideoManager.getInstance().getMediaPlayerListener() != null) {
            VideoManager.getInstance().getMediaPlayerListener().onVideoResume(seek);
        }
    }
}
