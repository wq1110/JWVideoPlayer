package com.media.jwvideoplayer.player.base.listener;

import android.view.Surface;
import com.media.jwvideoplayer.player.base.IPlayerManager;
import java.io.File;
import java.util.Map;

/**
 * Created by Joyce.wang on 2023/3/15.
 * Manager 与 View之间的接口
 */
public interface VideoViewBridge {
    /**
     * 开始准备播放
     *
     * @param url         播放url
     * @param headers 头部信息
     * @param loop        是否循环
     * @param speed       播放速度
     * @param cache       是否缓存
     * @param cachePath   缓存目录，可以为空，为空时使用默认
     */
    void prepare(final String url, final Map<String, String> headers, boolean loop, float speed, boolean cache, File cachePath);

    /**
     * 开始准备播放
     *
     * @param url         播放url
     * @param headers 头部信息
     * @param loop        是否循环
     * @param speed       播放速度
     * @param cache       是否缓存
     * @param cachePath   缓存目录，可以为空，为空时使用默认
     * @param overrideExtension   是否需要覆盖拓展类型
     */
    void prepare(final String url, final Map<String, String> headers, boolean loop, float speed, boolean cache, File cachePath, String overrideExtension);

    /**
     * 获取当前播放内核
     */
    IPlayerManager getPlayer();

    void start();

    void stop();

    void pause();

    int getVideoWidth();

    int getVideoHeight();

    boolean isPlaying();

    void seekTo(long time);

    long getCurrentPosition();

    long getDuration();

    int getVideoSarNum();

    int getVideoSarDen();

    /**
     * Surface是否支持外部lockCanvas，来自定义暂停时的绘制画面
     * exoplayer目前不支持，因为外部lock后，切换surface会导致异常
     */
    boolean isSurfaceSupportLockCanvas();

    //针对某些内核，缓冲百分比
    int getBufferedPercentage();

    //释放播放器
    void releaseMediaPlayer();

    void setCurrentVideoHeight(int currentVideoHeight);

    void setCurrentVideoWidth(int currentVideoWidth);

    int getCurrentVideoWidth();

    int getCurrentVideoHeight();

    void setNeedMute(boolean needMute);

    //设置渲染
    void setDisplay(Surface holder);

    void releaseSurface(Surface surface);

    //网络速度
    long getNetSpeed();

    //播放速度修改
    void setSpeed(float speed, boolean soundTouch);

    //播放速度修改
    void setSpeedPlaying(float speed, boolean soundTouch);

    //获取Rotate选择的flag，目前只有ijk用到
    int getRotateInfoFlag();

    MediaPlayerListener getMediaPlayerListener();

    void setMediaPlayerListener(MediaPlayerListener listener);
}
