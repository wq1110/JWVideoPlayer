package com.media.jwvideoplayer.player.base;

import android.content.Context;
import android.os.Message;
import com.media.jwvideoplayer.player.base.model.IjkOptionModel;
import java.util.List;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Joyce.wang on 2023/3/15.
 * 播放器管理接口
 */
public interface IPlayerManager {
    IMediaPlayer getMediaPlayer();

    /**
     * 初始化播放内核
     * @param message         播放器所需初始化内容
     * @param optionModelList 配置信息
     */
    void initVideoPlayer(Context context, Message message, List<IjkOptionModel> optionModelList);

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

    //设置渲染显示
    void showDisplay(Message msg);

    //是否静音
    void setNeedMute(boolean needMute);

    //单独设置 setVolume ，和 setNeedMute 互斥 float 0.0 - 1.0
    void setVolume(float left, float right);

    //释放渲染
    void releaseSurface();

    //释放内核
    void release();

    //缓存进度
    int getBufferedPercentage();

    //网络速度
    long getNetSpeed();

    //播放速度
    void setSpeedPlaying(float speed, boolean soundTouch);

    /**
     * Surface是否支持外部lockCanvas，来自定义暂停时的绘制画面
     * exoplayer目前不支持，因为外部lock后，切换surface会导致异常
     */
    boolean isSurfaceSupportLockCanvas();

    void setSpeed(float speed, boolean soundTouch);
}
