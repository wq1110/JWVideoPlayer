package com.media.jwvideoplayer.player.base;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.lib.provider.ContextProvider;
import com.media.jwvideoplayer.player.base.listener.IPlayerInitSuccessListener;
import com.media.jwvideoplayer.player.base.listener.MediaPlayerListener;
import com.media.jwvideoplayer.player.base.listener.VideoViewBridge;
import com.media.jwvideoplayer.player.base.model.IjkOptionModel;
import com.media.jwvideoplayer.player.base.model.VideoModel;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Joyce.wang on 2023/3/15.
 * video基类管理器
 */
public abstract class BaseVideoManager implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener, VideoViewBridge {
    private static final Logger logger = LoggerFactory.getLogger(BaseVideoManager.class.getName());

    protected static final int HANDLER_PREPARE = 0;
    protected static final int HANDLER_SETDISPLAY = 1;
    protected static final int HANDLER_RELEASE = 2;
    protected static final int HANDLER_RELEASE_SURFACE = 3;

    protected static final int BUFFER_TIME_OUT_ERROR = -192;//外部超时错误码

    /**
     当前播放的视频宽的高
     */
    protected int currentVideoWidth = 0;

    /**
     当前播放的视屏的高
     */
    protected int currentVideoHeight = 0;
    /**
     播放超时
     */
    protected int timeOut = 8 * 1000;
    /**
     缓冲比例
     */
    protected int bufferPoint;
    /**
     是否需要外部超时判断
     */
    protected boolean needTimeOutOther;
    /**
     是否需要静音
     */
    protected boolean needMute = false;

    /**
     ijkplayer option参数配置
     */
    protected List<IjkOptionModel> optionModelList;

    protected WeakReference<MediaPlayerListener> listener;
    protected IPlayerInitSuccessListener playerInitSuccessListener;

    //播放内核管理
    protected IPlayerManager playerManager;
    protected BaseVideoHandler mBaseVideoHandler;
    protected Handler mMainThreadHandler;

    protected void init() {
        mBaseVideoHandler = new BaseVideoHandler(Looper.getMainLooper());
        mMainThreadHandler = new Handler();
    }

    protected IPlayerManager getPlayManager() {
        return PlayerFactory.getPlayManager();
    }

    private void initVideo(Message msg) {
        try {
            currentVideoWidth = 0;
            currentVideoHeight = 0;

            if (playerManager != null) {
                playerManager.release();
            }
            playerManager = getPlayManager();

            if (playerManager instanceof BasePlayerManager) {
                ((BasePlayerManager) playerManager)
                        .setPlayerInitSuccessListener(playerInitSuccessListener);
            }
            playerManager.initVideoPlayer(ContextProvider.getContext(), msg, optionModelList);

            setNeedMute(needMute);
            IMediaPlayer mediaPlayer = playerManager.getMediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prepare(String url, Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath) {
        prepare(url, mapHeadData, loop, speed, cache, cachePath, null);
    }

    @Override
    public void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath, String overrideExtension) {
        if (TextUtils.isEmpty(url)) return;
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        VideoModel videoModel = new VideoModel(url, mapHeadData, loop, speed, cache, cachePath, overrideExtension);
        msg.obj = videoModel;
        sendMessage(msg);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getMediaPlayerListener() != null) {
                    getMediaPlayerListener().onPrepared(mp);
                }
            }
        });
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getMediaPlayerListener() != null) {
                    getMediaPlayerListener().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getMediaPlayerListener() != null) {
                    getMediaPlayerListener().onSeekComplete(mp);
                }
            }
        });
    }

    @Override
    public boolean onError(IMediaPlayer mp, final int what, final int extra) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getMediaPlayerListener() != null) {
                    getMediaPlayerListener().onError(mp, what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (needTimeOutOther) {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        startTimeOutBuffer();
                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        cancelTimeOutBuffer();
                    }
                }
                if (getMediaPlayerListener() != null) {
                    getMediaPlayerListener().onInfo(mp, what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        currentVideoWidth = mp.getVideoWidth();
        currentVideoHeight = mp.getVideoHeight();
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getMediaPlayerListener() != null) {
                    getMediaPlayerListener().onVideoSizeChanged();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, final int percent) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getMediaPlayerListener() != null) {
                    if (percent > bufferPoint) {
                        getMediaPlayerListener().onBufferingUpdate(mp, percent);
                    } else {
                        getMediaPlayerListener().onBufferingUpdate(mp, bufferPoint);
                    }
                }
            }
        });
    }

    @Override
    public void setCurrentVideoHeight(int currentVideoHeight) {
        this.currentVideoHeight = currentVideoHeight;
    }

    @Override
    public void setCurrentVideoWidth(int currentVideoWidth) {
        this.currentVideoWidth = currentVideoWidth;
    }

    @Override
    public int getCurrentVideoWidth() {
        return currentVideoWidth;
    }

    @Override
    public int getCurrentVideoHeight() {
        return currentVideoHeight;
    }


    @Override
    public long getNetSpeed() {
        if (playerManager != null) {
            return playerManager.getNetSpeed();
        }
        return 0;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {
        if (playerManager != null) {
            playerManager.setSpeedPlaying(speed, soundTouch);
        }
    }

    @Override
    public IPlayerManager getPlayer() {
        return playerManager;
    }

    @Override
    public int getBufferedPercentage() {
        if (playerManager != null) {
            return playerManager.getBufferedPercentage();
        }
        return 0;
    }

    @Override
    public void start() {
        if (playerManager != null) {
            playerManager.start();
        }
    }

    @Override
    public void stop() {
        if (playerManager != null) {
            playerManager.stop();
        }
    }

    @Override
    public void pause() {
        if (playerManager != null) {
            playerManager.pause();
        }
    }

    @Override
    public int getVideoWidth() {
        if (playerManager != null) {
            return playerManager.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (playerManager != null) {
            return playerManager.getVideoHeight();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        if (playerManager != null) {
            return playerManager.isPlaying();
        }
        return false;
    }

    @Override
    public void seekTo(long time) {
        if (playerManager != null) {
            playerManager.seekTo(time);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (playerManager != null) {
            return playerManager.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (playerManager != null) {
            return playerManager.getDuration();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (playerManager != null) {
            return playerManager.getVideoSarNum();
        }
        return 0;
    }

    @Override
    public int getVideoSarDen() {
        if (playerManager != null) {
            return playerManager.getVideoSarDen();
        }
        return 0;
    }

    @Override
    public int getRotateInfoFlag() {
        return IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED;
    }


    @Override
    public boolean isSurfaceSupportLockCanvas() {
        if (playerManager != null) {
            return playerManager.isSurfaceSupportLockCanvas();
        }
        return false;
    }

    public boolean isNeedMute() {
        return needMute;
    }

    /**
     是否需要静音
     */
    @Override
    public void setNeedMute(boolean needMute) {
        this.needMute = needMute;
        if (playerManager != null) {
            playerManager.setNeedMute(needMute);
        }
    }


    @Override
    public void setSpeed(float speed, boolean soundTouch) {
        if (playerManager != null) {
            playerManager.setSpeed(speed, soundTouch);
        }
    }

    @Override
    public void setDisplay(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_SETDISPLAY;
        msg.obj = holder;
        showDisplay(msg);
    }

    @Override
    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        sendMessage(msg);
    }

    @Override
    public void releaseSurface(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE_SURFACE;
        msg.obj = holder;
        sendMessage(msg);
    }

    protected void sendMessage(Message message) {
        mBaseVideoHandler.sendMessage(message);
    }

    /**
     后面再修改设计模式吧，现在先用着
     */
    private void showDisplay(Message msg) {
        if (playerManager != null) {
            playerManager.showDisplay(msg);
        }
    }
    /**
     启动十秒的定时器进行 缓存操作
     */
    protected void startTimeOutBuffer() {
        // 启动定时
        logger.e("startTimeOutBuffer");
        mMainThreadHandler.postDelayed(mTimeOutRunnable, timeOut);

    }

    public List<IjkOptionModel> getOptionModelList() {
        return optionModelList;
    }

    /**
     设置IJK视频的option
     */
    public void setOptionModelList(List<IjkOptionModel> optionModelList) {
        this.optionModelList = optionModelList;
    }

    /**
     取消 十秒的定时器进行 缓存操作
     */
    protected void cancelTimeOutBuffer() {
        logger.e("cancelTimeOutBuffer");
        // 取消定时
        if (needTimeOutOther)
            mMainThreadHandler.removeCallbacks(mTimeOutRunnable);
    }

    public int getTimeOut() {
        return timeOut;
    }

    public boolean isNeedTimeOutOther() {
        return needTimeOutOther;
    }

    /**
     是否需要在buffer缓冲时，增加外部超时判断
     <p>
     超时后会走onError接口，播放器通过onPlayError回调出
     <p>
     错误码为 ： BUFFER_TIME_OUT_ERROR = -192
     <p>
     由于onError之后执行GSYVideoPlayer的OnError，如果不想触发错误，
     可以重载onError，在super之前拦截处理。
     <p>
     public void onError(int what, int extra){
     do you want before super and return;
     super.onError(what, extra)
     }

     @param timeOut          超时时间，毫秒 默认8000
     @param needTimeOutOther 是否需要延时设置，默认关闭
     */
    public void setTimeOut(int timeOut, boolean needTimeOutOther) {
        this.timeOut = timeOut;
        this.needTimeOutOther = needTimeOutOther;
    }

    public IPlayerInitSuccessListener getPlayerPreparedSuccessListener() {
        return playerInitSuccessListener;
    }

    /**
     播放器初始化后接口
     */
    public void setPlayerInitSuccessListener(IPlayerInitSuccessListener listener) {
        this.playerInitSuccessListener = listener;
    }


    @Override
    public MediaPlayerListener getMediaPlayerListener() {
        if (listener == null)
            return null;
        return listener.get();
    }

    @Override
    public void setMediaPlayerListener(MediaPlayerListener listener) {
        if (listener == null)
            this.listener = null;
        else
            this.listener = new WeakReference<>(listener);
    }

    private Runnable mTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (listener != null) {
                logger.e( "time out for error listener");
                getMediaPlayerListener().onError(getPlayer().getMediaPlayer(), BUFFER_TIME_OUT_ERROR, BUFFER_TIME_OUT_ERROR);
            }
        }
    };

    private class BaseVideoHandler extends Handler {
        BaseVideoHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    initVideo(msg);
                    if (needTimeOutOther) {
                        startTimeOutBuffer();
                    }
                    break;
                case HANDLER_SETDISPLAY:
                    break;
                case HANDLER_RELEASE:
                    if (playerManager != null) {
                        playerManager.release();
                    }
                    bufferPoint = 0;
                    setNeedMute(false);
                    cancelTimeOutBuffer();
                    break;
                case HANDLER_RELEASE_SURFACE:
                    if (msg.obj != null) {
                        if (playerManager != null) {
                            playerManager.releaseSurface();
                        }
                    }
                    break;
            }
        }
    }
 }
