package com.media.jwvideoplayer.player.ui;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.R;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.player.PlayerHelper;
import com.media.jwvideoplayer.player.base.ExoPlayerManager;
import com.media.jwvideoplayer.player.base.IjkPlayerManager;
import com.media.jwvideoplayer.player.base.LayoutQuery;
import com.media.jwvideoplayer.player.base.PlayerFactory;
import com.media.jwvideoplayer.player.base.SystemPlayerManager;
import com.media.jwvideoplayer.player.base.listener.MediaPlayerListener;
import com.media.jwvideoplayer.player.base.listener.VideoViewBridge;
import com.media.jwvideoplayer.player.base.render.MediaTextureRenderView;
import com.media.jwvideoplayer.player.listener.ConnectionChangeListener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Joyce.wang on 2023/3/16.
 * 视频回调与状态处理等相关层
 */
public abstract class VideoView extends MediaTextureRenderView implements MediaPlayerListener {
    private static Logger logger = LoggerFactory.getLogger(VideoView.class.getName());
    //初始化状态
    public static final int CURRENT_STATE_IDLE = -1;
    //正常
    public static final int CURRENT_STATE_NORMAL = 0;
    //准备中
    public static final int CURRENT_STATE_PREPAREING = 1;
    //播放中
    public static final int CURRENT_STATE_PLAYING = 2;
    //开始缓冲
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    //暂停
    public static final int CURRENT_STATE_PAUSE = 5;
    //自动播放结束
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    //错误状态
    public static final int CURRENT_STATE_ERROR = 7;

    //audiotrack block error
    public static final int FFP_MSG_ERROR_997 = 997;
    public static final int FFP_MSG_ERROR_998 = 998;

    public static final int MEDIA_INFO_PLAYER_TYPE = 1000001;

    protected Activity mActivity;//依附的容器Activity
    protected Context mContext;
    //Activity界面的中布局的查询器
    protected LayoutQuery query;
    //屏幕宽度
    protected int mScreenWidth;
    //屏幕高度
    protected int mScreenHeight;
    //音频焦点的监听
    protected AudioManager mAudioManager;
    //转化后的URL
    protected String mUrl;
    //循环
    protected boolean mLooping = false;
    //播放速度
    protected float mSpeed = 1;
    //是否播边边缓冲
    protected boolean mCache = false;
    //缓存路径，可不设置
    protected File mCachePath;
    // 是否需要覆盖拓展类型
    protected String mOverrideExtension;
    //当前的播放状态
    protected int mCurrentState = CURRENT_STATE_IDLE;
    protected String format;
    //是否是预告片
    protected boolean isTrailer;
    //是否是剧集
    protected boolean isSeries = false;
    //Prepared
    protected boolean isPrepared = false;
    //是否是播放缓存完成的影片
    protected boolean isCacheFinishFilm = false;
    //从哪个开始播放
    protected long mSeekOnStart = -1;
    //是否准备完成前调用了暂停
    protected boolean mPauseBeforePrepared = false;
    //当前的播放位置
    protected long mCurrentPosition;
    //当前是否全屏
    protected boolean mIfCurrentIsFullscreen = false;
    //是否播放器当失去音频焦点
    protected boolean mReleaseWhenLossAudio = true;
    protected boolean isPortrait = true;

    protected OnVodMediaPlayerListener onEventCallback;
    //http request header
    protected Map<String, String> mHeaders = new HashMap<>();


    protected boolean isManualSeeking = false;
    protected boolean hasSeeking = false;
    protected long seekCompleteTime;
    //播放缓冲监听
    private int mCurrentBufferPercentage;

    public VideoView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    protected void init(Context context) {
        mContext = context;
        mActivity = (Activity) context;
        query = new LayoutQuery((Activity) mContext);
        initInflate(mContext);
        mTextureViewContainer = (ViewGroup) findViewById(R.id.surface_container);
        if (isInEditMode())
            return;
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

    }

    protected void initInflate(Context context) {
        try {
            View.inflate(context, getLayoutId(), this);
        } catch (InflateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCurrentVideoWidth() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getCurrentVideoHeight() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoHeight();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoSarNum();
        }
        return 0;
    }

    @Override
    public int getVideoSarDen() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoSarDen();
        }
        return 0;
    }

    /**
     * seekto what you want
     */
    public void seekTo(long position) {
        try {
            if (getVideoManager() != null && position > 0) {
                getVideoManager().seekTo(position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setVideoUrl(String url) {
        setVideoUrl(url, null);
    }

    public void setVideoUrl(String url, Map<String, String> headers) {
        this.mUrl = url;
        this.mHeaders = headers;
        mCurrentState = CURRENT_STATE_NORMAL;
        prepareVideo();
    }

    /**
     * 开始状态视频播放
     */
    protected void prepareVideo() {
        startPrepare();
    }

    protected void startPrepare() {
        getVideoManager().setMediaPlayerListener(this);
        setPlayType();
        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        getVideoManager().prepare(mUrl, (mHeaders == null) ? new HashMap<String, String>() : mHeaders, mLooping, mSpeed, mCache, mCachePath, mOverrideExtension);
        setStateAndUi(CURRENT_STATE_PREPAREING);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        if (mCurrentState != CURRENT_STATE_PREPAREING) {
            return;
        }
        isPrepared = true;
        onPreparedEvent(mp);

        startAfterPrepared();
        if (onEventCallback != null) onEventCallback.onPrepared(mp);
    }

    //prepared成功之后会开始播放
    public void startAfterPrepared() {
        if (!isPrepared) {
            prepareVideo();
        }

        try {
            if (getVideoManager() != null) {
                getVideoManager().start();
            }

            setStateAndUi(CURRENT_STATE_PLAYING);

            if (getVideoManager() != null && mSeekOnStart > 0) {
                getVideoManager().seekTo(mSeekOnStart);
                mSeekOnStart = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        addTextureView();

        if (mPauseBeforePrepared) {
            onVideoPause();
            mPauseBeforePrepared = false;
        }
    }

    /**
     * 暂停状态
     */
    @Override
    public void onVideoPause() {
        if (mCurrentState == CURRENT_STATE_PREPAREING) {
            mPauseBeforePrepared = true;
        }
        try {
            if (getVideoManager() != null &&
                    getVideoManager().isPlaying()) {
                setStateAndUi(CURRENT_STATE_PAUSE);
                mCurrentPosition = getVideoManager().getCurrentPosition();
                if (getVideoManager() != null)
                    getVideoManager().pause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复暂停状态
     */
    @Override
    public void onVideoResume() {
        onVideoResume(true);
    }
    /**
     * 恢复暂停状态
     *
     * @param seek 是否产生seek动作
     */
    @Override
    public void onVideoResume(boolean seek) {
        mPauseBeforePrepared = false;
        if (mCurrentState == CURRENT_STATE_PAUSE) {
            try {
                if (mCurrentPosition >= 0 && getVideoManager() != null) {
                    if (seek) {
                        getVideoManager().seekTo(mCurrentPosition);
                    }
                    getVideoManager().start();
                    setStateAndUi(CURRENT_STATE_PLAYING);
                    if (mAudioManager != null && !mReleaseWhenLossAudio) {
                        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    }
                    mCurrentPosition = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAutoCompletion() {
        setStateAndUi(CURRENT_STATE_AUTO_COMPLETE);
        mCurrentPosition = 0;

        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        if (!mIfCurrentIsFullscreen) getVideoManager().setMediaPlayerListener(null);
        mAudioManager.abandonAudioFocus(null);

        onCompletionEvent(getVideoManager().getPlayer().getMediaPlayer());
    }

    @Override
    public void onCompletion() {
        //make me normal first
        setStateAndUi(CURRENT_STATE_IDLE);

        mCurrentPosition = 0;

        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        if (!mIfCurrentIsFullscreen) {
            getVideoManager().setMediaPlayerListener(null);
        }
        getVideoManager().setCurrentVideoHeight(0);
        getVideoManager().setCurrentVideoWidth(0);

        mAudioManager.abandonAudioFocus(null);
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        isManualSeeking = false;
        hasSeeking = true;
        seekCompleteTime = SystemClock.elapsedRealtime();
        if (onEventCallback != null) onEventCallback.onSeekCompleted(mp.getDuration(), mp.getCurrentPosition());
    }

    @Override
    public void onError(IMediaPlayer mp, int what, int extra) {
        if (VideoView.FFP_MSG_ERROR_997 != extra && VideoView.FFP_MSG_ERROR_998 != extra) {
            mCurrentState = CURRENT_STATE_ERROR;
        }
        onErrorEvent(mp, what, extra);
    }


    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        mCurrentBufferPercentage = percent;
    }

    @Override
    public void onInfo(IMediaPlayer mp, int what, int extra) {
        onInfoEvent(mp, what, extra);
    }

    @Override
    public void onVideoSizeChanged() {
        int mVideoWidth = getVideoManager().getCurrentVideoWidth();
        int mVideoHeight = getVideoManager().getCurrentVideoHeight();
        if (mVideoWidth != 0 && mVideoHeight != 0 && mTextureView != null) {
            mTextureView.requestLayout();
        }
    }

    @Override
    protected void setDisplay(Surface surface) {
        getVideoManager().setDisplay(surface);
    }

    @Override
    protected void onSurfaceCreatedEvent(Surface surface) {
        seekTo(getCurrentPositionWhenPlaying());
    }

    @Override
    protected void onSurfaceChangedEvent(Surface surface, int width, int height) {

    }

    @Override
    protected void onSurfaceDestroyedEvent(Surface surface) {
        getVideoManager().releaseSurface(surface);
    }

    /**
     * 获取当前播放进度
     */
    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        if (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE) {
            try {
                position = (int) getVideoManager().getCurrentPosition();
            } catch (Exception e) {
                e.printStackTrace();
                return position;
            }
        }
        if (position == 0 && mCurrentPosition > 0) {
            return (int) mCurrentPosition;
        }
        return position;
    }

    /**
     * 获取当前总时长
     */
    public int getDuration() {
        int duration = 0;
        try {
            duration = (int) getVideoManager().getDuration();
        } catch (Exception e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    public int getPlayerCacheTime() {
        try {
            if (isInPlayingState()) {
                int currentPosition = (int) getCurrentPositionWhenPlaying();
                int cachePosition = (int) (mCurrentBufferPercentage * getDuration() / 100);
                int playerCacheTime = (cachePosition - currentPosition) / 1000;
                return Math.max(playerCacheTime, 0);
            }
        } catch (Throwable e) {
            logger.e("get player cache time error!!!");
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * 释放吧
     */
    public void release() {
        if (isCurrentMediaListener()) {
            releaseVideos();
        }
    }
    /**
     * 获取当前播放状态
     */
    public int getCurrentState() {
        return mCurrentState;
    }

    /**
     * 根据状态判断是否播放中
     */
    public boolean isInPlayingState() {
        return (mCurrentState != CURRENT_STATE_IDLE &&
                mCurrentState != CURRENT_STATE_NORMAL &&
                mCurrentState != CURRENT_STATE_PREPAREING &&
                mCurrentState != CURRENT_STATE_AUTO_COMPLETE &&
                mCurrentState != CURRENT_STATE_ERROR);
    }

    public boolean isPlaying() {
        return isInPlayingState() && getVideoManager().isPlaying();
    }

    public boolean isPaused(){
        return mCurrentState == CURRENT_STATE_PAUSE;
    }

    public void setOnEventCallback(OnVodMediaPlayerListener onEventCallback) {
        this.onEventCallback = onEventCallback;
    }

    private void setPlayType() {
        int playerType = PlayerHelper.getInstance().getPlayerTypeByFormat(format, isTrailer);
        switch (playerType) {
            case PlayerHelper.PV_PLAYER__IjkExoMediaPlayer:
                PlayerFactory.setPlayManager(ExoPlayerManager.class);
                break;
            case PlayerHelper.PV_PLAYER__AndroidMediaPlayer:
                PlayerFactory.setPlayManager(SystemPlayerManager.class);
                break;
            case PlayerHelper.PV_PLAYER__IjkMediaPlayer:
                PlayerFactory.setPlayManager(IjkPlayerManager.class);
                break;
        }
    }

    public void setCacheFilm(boolean isCacheFinishFilm) {
        this.isCacheFinishFilm = isCacheFinishFilm;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public void setHeaders(Map<String, String> headers) {
        if (headers != null) {
            this.mHeaders = headers;
        }
    }

    public void setNeedMute(boolean needMute) {
        if (getVideoManager() != null) {
            getVideoManager().setNeedMute(needMute);
        }
    }

    protected boolean isCurrentMediaListener() {
        return getVideoManager().getMediaPlayerListener() != null
                && getVideoManager().getMediaPlayerListener() == this;
    }

    public void setTrailer(boolean isTrailer) {
        this.isTrailer = isTrailer;
    }

    protected boolean isTrailer() {
        return this.isTrailer;
    }

    protected void setSeries(boolean isSeries) {
        this.isSeries = isSeries;
    }

    protected boolean isSeries() {
        return this.isSeries;
    }

    protected int getCurrentBufferPercentage() {
        return mCurrentBufferPercentage;
    }

    protected void onDestroy() {
        onEventCallback = null;
    }

    /**
     * 当前UI
     */
    public abstract int getLayoutId();

    /**
     * 获取管理器桥接的实现
     */
    public abstract VideoViewBridge getVideoManager();

    /**
     * 设置播放显示状态
     *
     * @param state
     */
    protected abstract void setStateAndUi(int state);

    /**
     * 释放播放器
     */
    protected abstract void releaseVideos();

    protected abstract void onPreparedEvent(IMediaPlayer mp);

    protected abstract boolean onInfoEvent(IMediaPlayer mp, int what, int extra);

    protected abstract boolean onErrorEvent(IMediaPlayer iMediaPlayer, int framework_err, int impl_err);

    protected abstract void onCompletionEvent(IMediaPlayer mp);

    public interface OnVodMediaPlayerListener {
        void onPreparePlay();

        void onNetworkChange(BaseVideoPlayer.Action action);

        void onNetworkChange(ConnectionChangeListener.ConnectionType type);

        void onNetworkDisconnected(BaseVideoPlayer.Action<Boolean> action);

        void onResourcesChange();

        void onSubtitlesChange();

        void onVideoEpisodeChange();

        void onNextEpisode();

        void onCompletion(IMediaPlayer iMediaPlayer);

        void onSeekCompleted(long duration, long position);

        void onError(IMediaPlayer iMediaPlayer, int position, int reason);

        void onClickTitleHelp();

        void onPrepared(IMediaPlayer mp);

        void onPlayerType(int playerType);

        void onClose();

        void onLocaleChange();

        void finishActivity();
        void onBufferingStart(IMediaPlayer mp);

        void onBufferingEnd(IMediaPlayer mp, boolean isPrepare, long bufferingStartTime,
                            long bufferingTime, long prepareFirstBufferingTime,
                            long seekFirstBuffingTime, long startBufferingPosition);
    }

    public interface Action<T> {
        void call(T t);
    }
}
