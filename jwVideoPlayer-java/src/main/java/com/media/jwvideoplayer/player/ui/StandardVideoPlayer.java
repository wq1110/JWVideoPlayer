package com.media.jwvideoplayer.player.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.media.jwvideoplayer.R;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.lib.provider.ContextProvider;
import com.media.jwvideoplayer.lib.utils.NetworkUtils;
import com.media.jwvideoplayer.player.VodMediaConstant;
import com.media.jwvideoplayer.player.base.render.VideoType;
import com.media.jwvideoplayer.player.listener.ConnectionChangeListener;
import com.media.jwvideoplayer.utils.VideoUtils;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Joyce.wang on 2023/3/20.
 * 标准播放器，继承之后实现一些ui显示效果，如显示／隐藏ui，播放按键等
 */
public class StandardVideoPlayer extends VideoPlayer implements SeekBar.OnSeekBarChangeListener {
    private static Logger logger = LoggerFactory.getLogger(StandardVideoPlayer.class.getName());

    private static final long PLAY_NEXT_EPISODE_THRESHOLD_VALUE = 60 * 1000;
    public static final String KEY_LOADING_EXTRA_INFO = "key_loading_extra_info";

    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private TextView mEndTime;
    private TextView mediaNextEpisode;
    private View mTopBox;
    private View mRightBox;
    private ImageView mFastForwardIcon;
    private ImageView mFastRewindIcon;
    private TextView mUseMobileDataTxt;
    private View mBottomBox;
    //loading box begin
    private View mLoadingBox;
    private TextView mLoadingPercent;
    private TextView mLoadingSpeed;
    private TextView mLoadingExtraInfo;
    //loading box end
    private ImageView mPlayIcon;
    private ImageView mPlay;
    private ImageView mIvTitleHelp;
    private View mExtraBox;
    private TextView mVodTitle;
    private View mVideos;
    private ImageView mMediaBackIv;
    private View mResources;
    private ImageView mZoom;

    private boolean isAllowDisplayNextEpisode = true;//是否允许展示next episode
    private long delayTimeToHideControlbox = 5 * 1000;

    private long bufferingStartTime;
    private long bufferingEndTime;
    private long bufferingTime;
    private long prepareTime;
    private long prepareFirstBufferingTime;
    private long seekFirstBuffingTime;

    private boolean hasFirstPrepareBuffering = false;
    private long startBufferingPosition = -1;
    private boolean isBufferingBySeek = false;
    private boolean isHandPause;

    private int initBottomBoxHeight;

    public StandardVideoPlayer(@NonNull Context context) {
        super(context);
    }

    public StandardVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StandardVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StandardVideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onHandleMessage(Message msg) {
        super.onHandleMessage(msg);
        switch (msg.what) {
            case VodMediaConstant.MESSAGE_UPDATE_PROGRESS:
                long pos = syncProgress();
                msg = mHandler.obtainMessage(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
                mHandler.sendMessageDelayed(msg, 1000 - (pos % 1000));
                break;
            case VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX:
                hideBottom();
                hideTop();
                hideRight();
                hideFastForwardIcon();
                hideFastRewindIcon();
                if (isInPlayingState()) hidePlayIcon();
                break;
            case VodMediaConstant.MESSAGE_HIDE_USE_MOBILE_DATA_TIPS:
                hideUseMobileDataTips();
                break;
            case VodMediaConstant.MESSAGE_SHOW_BOTTOM:
                if (mBottomBox.getVisibility() == GONE) {
                    mBottomBox.setVisibility(VISIBLE);
                }
                break;
            case VodMediaConstant.MESSAGE_HIDE_BOTTOM:
                if (mBottomBox.getVisibility() == VISIBLE) mBottomBox.setVisibility(GONE);
                break;
            case VodMediaConstant.MESSAGE_SHOW_LOADING:
                clearLoading();
                if (!isShowLoadingBox()) mLoadingBox.setVisibility(VISIBLE);
                break;
            case VodMediaConstant.MESSAGE_HIDE_LOADING:
                clearLoading();
                if (isShowLoadingBox()) mLoadingBox.setVisibility(GONE);
                break;
            case VodMediaConstant.MESSAGE_CHANGE_PLAY_STATUS:
                statusChange(msg.arg1);
                break;
            case VodMediaConstant.MESSAGE_SHOW_PLAY_ICON:
                mPlayIcon.setVisibility(VISIBLE);
                break;
            case VodMediaConstant.MESSAGE_HIDE_PLAY_ICON:
                mPlayIcon.setVisibility(INVISIBLE);
                break;
            case VodMediaConstant.MESSAGE_UPDATE_LOADING_EXTRA_INFO:
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    String loadingExtraInfo = bundle.getString(KEY_LOADING_EXTRA_INFO);
                    if (currentConnectionType != ConnectionChangeListener.ConnectionType.NONE) {
                        mLoadingExtraInfo.setText(loadingExtraInfo);
                    }
                }
                break;
            case VodMediaConstant.MESSAGE_SHOW_NEXT_EPISODE:
                if (mediaNextEpisode.getVisibility() == GONE) {
                    mediaNextEpisode.setVisibility(VISIBLE);
                }
                break;
            case VodMediaConstant.MESSAGE_HIDE_NEXT_EPISODE:
                if (mediaNextEpisode.getVisibility() == VISIBLE) {
                    mediaNextEpisode.setVisibility(GONE);
                }
                break;
        }
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mSeekBar = findViewById(R.id.media_seekBar);
        mCurrentTime = findViewById(R.id.media_currentTime);
        mEndTime = findViewById(R.id.media_endTime);

        mediaNextEpisode = findViewById(R.id.media_next_episode);
        mTopBox = findViewById(R.id.media_top_box);
        mRightBox = findViewById(R.id.layout_right_box);
        mFastForwardIcon = findViewById(R.id.media_fast_forward_icon);
        mFastRewindIcon = findViewById(R.id.media_fast_rewind_icon);

        mUseMobileDataTxt = findViewById(R.id.use_mobile_data_txt);
        mBottomBox = findViewById(R.id.media_bottombox);
        // loading box begin
        mLoadingBox = findViewById(R.id.media_loading_box);
        mLoadingPercent = findViewById(R.id.media_loading_percent);
        mLoadingSpeed = findViewById(R.id.media_loading_speed);
        mLoadingExtraInfo = findViewById(R.id.media_loading_extra_info);
        // loading box end
        mPlayIcon = findViewById(R.id.media_play_icon);

        mPlay = findViewById(R.id.media_play);
        mIvTitleHelp = findViewById(R.id.iv_play_title_help);

        mExtraBox = findViewById(R.id.layout_extra_box);
        mVodTitle = findViewById(R.id.vod_title);
        mVideos = findViewById(R.id.layout_episode);

        mMediaBackIv = findViewById(R.id.media_back_iv);
        mResources = findViewById(R.id.layout_resource);
        mZoom = findViewById(R.id.btn_zoom);

        mPlay.setOnClickListener(this);
        mResources.setOnClickListener(this);
        mZoom.setOnClickListener(this);
        mVideos.setOnClickListener(this);
        mMediaBackIv.setOnClickListener(this);
        mPlayIcon.setOnClickListener(this);
        mFastForwardIcon.setOnClickListener(this);
        mFastRewindIcon.setOnClickListener(this);
        mIvTitleHelp.setOnClickListener(this);
        mediaNextEpisode.setOnClickListener(this);

        mSeekBar.setProgress(0);
        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(this);

        initBottomBoxHeight = mBottomBox.getLayoutParams().height;
    }

    //处理视频窗口单击事件
    @Override
    public void onSingleTapUpEvent() {
        mHandler.removeMessages(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX);
        if (VISIBLE == mBottomBox.getVisibility()) {
            hideTop();
            hideBottom();
            hideRight();
            hideFastForwardIcon();
            hideFastRewindIcon();
            if (!isPortrait && isInPlayingState()) hidePlayIcon();
        } else {
            showTop();
            if (isInPlayingState()) {
                showBottom();
            }

            showRight();
            showFastForwardIcon();
            showFastRewindIcon();
            if (!isPortrait && isInPlayingState()) showPlayIcon();

            mHandler.sendEmptyMessageDelayed(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX, delayTimeToHideControlbox);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int id = v.getId();
        if (R.id.media_play == id) {
            switchPlaybackStatus();
        } else if (R.id.btn_zoom == id) {
            toggleRatio();
        } else if (R.id.layout_episode == id) {
            showEpisodes();
        } else if (R.id.media_back_iv == id) {
            if (!isPortrait) {
                toggleFullScreen();
            } else {
                clickBack();
            }
        } else if (R.id.media_play_icon == id) {
            clickPlayIcon();
        } else if (R.id.iv_play_title_help == id) {
            clickTitleHelp();
        } else if (R.id.layout_resource == id) {
            changeResources();
        } else if (R.id.media_fast_forward_icon == id) {
            handleFixedStep(10000);
        } else if (R.id.media_fast_rewind_icon == id) {
            handleFixedStep(-10000);
        } else if (R.id.media_next_episode == id) {
            clickNextEpisode();
        }
    }

    //处理快进快退固定时长
    public void handleFixedStep(long step) {
        if (!isInPlayingState() || step == 0) {
            return;
        }

        mHandler.removeMessages(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
        mHandler.removeMessages(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX);

        long currentPosition = getCurrentPositionWhenPlaying();
        long duration = getDuration();
        long finalPosition = currentPosition;
        if (duration > 0) {
            if (step > 0) {
                if (duration - currentPosition > step) {
                    finalPosition += step;
                } else {
                    finalPosition = duration;
                }
            } else {
                if (currentPosition > Math.abs(step)) {
                    finalPosition -= Math.abs(step);
                } else {
                    finalPosition = 0;
                }
            }
        } else {
            logger.i("handleFixedStep duration[%d] is invalid", duration);
        }

        if (finalPosition != currentPosition) {
            seekTo((int)finalPosition);
        }

        mHandler.removeMessages(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
    }

    public void toggleRatio() {
        if (VideoType.getShowType() == VideoType.SCREEN_TYPE_FULL) {
            VideoType.setShowType(VideoType.SCREEN_TYPE_DEFAULT);
        } else {
            VideoType.setShowType(VideoType.SCREEN_TYPE_FULL);
        }
        changeTextureViewShowType();
        if (mTextureView != null) mTextureView.requestLayout();
    }

    public void clickNextEpisode() {
        isAllowDisplayNextEpisode = false;
        hideNextEpisode();
        if (null != onEventCallback) {
            onEventCallback.onNextEpisode();
        }
    }

    private void switchPlaybackStatus() {
        if (isPlaying()) {
            isHandPause = true;
            onVideoPause();
        } else {
            isHandPause = false;
            if ((ConnectionChangeListener.ConnectionType.MOBILE == vodMediaReceiver.getType()) && (!VodMediaConstant.isMobileTraffic)) {
                if (null != onEventCallback) {
                    onEventCallback.onNetworkChange(new BaseVideoPlayer.Action<Boolean>() {
                        @Override
                        public void call(Boolean o) {
                            if (o) {
                                onVideoResume();
                            }
                        }
                    });
                }
            } else if (ConnectionChangeListener.ConnectionType.NONE == vodMediaReceiver.getType()) {
                if (null != onEventCallback) {
                    onEventCallback.onNetworkDisconnected(new BaseVideoPlayer.Action<Boolean>() {
                        @Override
                        public void call(Boolean isCacheFilm) {
                            if (isCacheFilm) {
                                onVideoResume();
                            }
                        }
                    });
                }
            } else {
                onVideoResume();
            }
        }
    }

    private void showEpisodes() {
        if (null != onEventCallback) {
            onEventCallback.onVideoEpisodeChange();
        }
    }

    public void clickPlayIcon() {
        if (isInPlayingState()) {
            //播放中是播放暂停切换
            switchPlaybackStatus();
        } else {
            if (null != onEventCallback) onEventCallback.onPreparePlay();
        }
    }

    public void changeResources() {
        if (null != onEventCallback) {
            onEventCallback.onResourcesChange();
        }
    }

    public void clickTitleHelp() {
        if (null != onEventCallback) {
            onEventCallback.onClickTitleHelp();
        }
    }

    public void clickBack() {
        if (null != onEventCallback) {
            onEventCallback.finishActivity();
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.m_play_layout_media_controller;
    }

    @Override
    protected void onPreparedEvent(IMediaPlayer mp) {
        prepareTime = SystemClock.elapsedRealtime();
        if (isPortrait) hidePlayFeedback();
        hidePlayIcon();
        hideLoadingBox();
    }

    @Override
    protected boolean onInfoEvent(IMediaPlayer mp, int what, int extra) {
        logger.i("onInfoEvent what: %s", what);
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                bufferingStartTime = SystemClock.elapsedRealtime();
                if (isPrepared && !hasFirstPrepareBuffering) {
                    hasFirstPrepareBuffering = true;
                    prepareFirstBufferingTime = SystemClock.elapsedRealtime() - prepareTime;
                }

                if (hasSeeking) {
                    hasSeeking = false;
                    seekFirstBuffingTime = SystemClock.elapsedRealtime() - seekCompleteTime;
                }
                startBufferingPosition = mp.getCurrentPosition();
                showLoadingBox();
                if (ConnectionChangeListener.ConnectionType.NONE == currentConnectionType) {
                    if (onEventCallback != null) {
                        onEventCallback.onNetworkDisconnected(new BaseVideoPlayer.Action<Boolean>() {
                            @Override
                            public void call(Boolean isCacheFilm) {
                                if (!isCacheFilm) {
                                    onVideoPause();
                                }
                            }
                        });
                    }
                }
                if (isManualSeeking) {
                    isBufferingBySeek = true;
                } else {
                    isBufferingBySeek = false;
                    if (onEventCallback != null) onEventCallback.onBufferingStart(mp);
                }
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                hideLoadingBox();
                bufferingEndTime = SystemClock.elapsedRealtime();
                bufferingTime = bufferingEndTime - bufferingStartTime;
                if (!isBufferingBySeek) {
                    if (onEventCallback != null) onEventCallback.onBufferingEnd(mp, isPrepared, bufferingStartTime,
                            bufferingTime, prepareFirstBufferingTime, seekFirstBuffingTime, startBufferingPosition);
                }
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
                if (mCurrentState == CURRENT_STATE_PLAYING) {
                    mHandler.sendEmptyMessageDelayed(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX, delayTimeToHideControlbox);
                } else if (mCurrentState == CURRENT_STATE_PAUSE) {
                    mHandler.removeMessages(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX);
                }
                break;
            case VideoView.MEDIA_INFO_PLAYER_TYPE:
                if (onEventCallback != null) onEventCallback.onPlayerType(extra);
                break;
        }

        return false;
    }

    @Override
    protected boolean onErrorEvent(IMediaPlayer iMediaPlayer, int framework_err, int impl_err) {
        hideNextEpisode();
        if (null != onEventCallback) {
            onEventCallback.onError(iMediaPlayer, 0, framework_err);
        }
        return false;
    }

    @Override
    protected void onCompletionEvent(IMediaPlayer mp) {
        hideNextEpisode();
        if (null != onEventCallback) {
            onEventCallback.onCompletion(mp);
        }
    }

    public void hideBottom() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_HIDE_BOTTOM);
    }

    public void showBottom() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_SHOW_BOTTOM);
    }

    public void hideTop() {
        this.mTopBox.setVisibility(GONE);
    }

    public void showTop() {
        this.mTopBox.setVisibility(VISIBLE);
    }

    public void hideRight() {
        this.mRightBox.setVisibility(GONE);
    }

    public void showRight() {
        if (!isPortrait) {
            this.mRightBox.setVisibility(VISIBLE);
        }
    }

    public void hideFastForwardIcon() {
        this.mFastForwardIcon.setVisibility(GONE);
    }

    public void showFastForwardIcon() {
        if (!isPortrait && isInPlayingState()) {
            this.mFastForwardIcon.setVisibility(VISIBLE);
        }
    }

    public void hideFastRewindIcon() {
        this.mFastRewindIcon.setVisibility(GONE);
    }

    public void showFastRewindIcon() {
        if (!isPortrait && isInPlayingState()) {
            this.mFastRewindIcon.setVisibility(VISIBLE);
        }
    }

    public void hideExtraBox() {
        if (isPortrait) {
            this.mExtraBox.setVisibility(GONE);
        } else {
            this.mExtraBox.setVisibility(INVISIBLE);
        }
    }

    public void showExtraBox() {
        this.mExtraBox.setVisibility(VISIBLE);
    }

    public void hideResource() {
        this.mResources.setVisibility(INVISIBLE);
    }

    public void showResource() {
        this.mResources.setVisibility(VISIBLE);
    }

    public void showVodTitle() {
        this.mVodTitle.setVisibility(VISIBLE);
    }

    public void hideVodTitle() {
        this.mVodTitle.setVisibility(GONE);
    }

    public void setAllowDisplayNextEpisode(boolean isAllowDisplayNextEpisode) {
        this.isAllowDisplayNextEpisode = isAllowDisplayNextEpisode;
    }

    public void showNextEpisode() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_SHOW_NEXT_EPISODE);
    }

    public void hideNextEpisode() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_HIDE_NEXT_EPISODE);
    }

    public void hidePlayIcon() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_HIDE_PLAY_ICON);
    }

    public void showPlayIcon() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_SHOW_PLAY_ICON);
    }

    public void hideUseMobileDataTips() {
        if (mUseMobileDataTxt != null) mUseMobileDataTxt.setVisibility(GONE);
    }

    public void showUseMobileDataTips() {
        if (NetworkUtils.isMobileNetwork(getContext())) {
            if (mUseMobileDataTxt != null) mUseMobileDataTxt.setVisibility(VISIBLE);
            mHandler.sendEmptyMessageDelayed(VodMediaConstant.MESSAGE_HIDE_USE_MOBILE_DATA_TIPS, 5000);
        }
    }

    public void showPlayFeedback() {
        this.mIvTitleHelp.setVisibility(View.VISIBLE);
    }

    public void hidePlayFeedback() {
        this.mIvTitleHelp.setVisibility(View.GONE);
    }

    public void showVideos() {
        this.mVideos.setVisibility(VISIBLE);
    }

    public void hideVideos() {
        this.mVideos.setVisibility(GONE);
    }

    public void setLoadingPercent(String txt) {
        if (currentConnectionType != ConnectionChangeListener.ConnectionType.NONE) {
            mLoadingPercent.setText(txt);
        }
    }

    public void setLoadingSpeed(String txt) {
        if (currentConnectionType != ConnectionChangeListener.ConnectionType.NONE) {
            mLoadingSpeed.setText(txt);
        }
    }

    public void setLoadingExtraInfo(String txt) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_LOADING_EXTRA_INFO, txt);
        Message message = mHandler.obtainMessage(VodMediaConstant.MESSAGE_UPDATE_LOADING_EXTRA_INFO);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    private void clearLoading() {
        this.mLoadingPercent.setText("");
        this.mLoadingSpeed.setText("");
        this.mLoadingExtraInfo.setText("");
    }

    public void showBackButton() {
        if (mMediaBackIv.getVisibility() == GONE) {
            mMediaBackIv.setVisibility(VISIBLE);
        }
    }

    public void hideBackButton() {
        if (mMediaBackIv.getVisibility() == VISIBLE) {
            mMediaBackIv.setVisibility(GONE);
        }
    }

    public void hidePlayButton() {
        this.mPlay.setVisibility(GONE);
    }

    public void showPlayButton() {
        this.mPlay.setVisibility(VISIBLE);
    }

    public void setVodTitle(String title) {
        mVodTitle.setText(title);
    }

    /*
     *  loading box controller
     */
    public void hideLoadingBox() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_HIDE_LOADING);
    }

    public void showLoadingBox() {
        mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_SHOW_LOADING);
    }

    public boolean isShowLoadingBox() {
        return this.mLoadingBox.getVisibility() == VISIBLE;
    }
    /**
     * 状态改变同步UI
     */
    private void statusChange(int newStatus) {
        logger.i("currentState:%d newState:%d", mCurrentState, newStatus);
        if (newStatus == CURRENT_STATE_IDLE) {
            mCurrentState = CURRENT_STATE_IDLE;
            updatePausePlay();
        } else if (newStatus == CURRENT_STATE_AUTO_COMPLETE) {
            mCurrentState = CURRENT_STATE_AUTO_COMPLETE;
            mCurrentPosition = 0;
            hideBottom();
            updatePausePlay();
        } else if (newStatus == CURRENT_STATE_PREPAREING) {
            mCurrentState = CURRENT_STATE_PREPAREING;
        } else if (newStatus == CURRENT_STATE_PAUSE) {
            mCurrentState = CURRENT_STATE_PAUSE;
            updatePausePlay();
            mHandler.removeMessages(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
            mHandler.removeMessages(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX);
            showTop();
            showBottom();
            showFastForwardIcon();
            showFastRewindIcon();
        } else if (newStatus == CURRENT_STATE_PLAYING) {
            mCurrentState = CURRENT_STATE_PLAYING;
            updatePausePlay();
            showTop();
            showBottom();
            showFastForwardIcon();
            showFastRewindIcon();
            mHandler.removeMessages(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
            mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
            mHandler.sendEmptyMessageDelayed(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX, delayTimeToHideControlbox);
        } else if (newStatus == CURRENT_STATE_ERROR) {
            mCurrentState = CURRENT_STATE_ERROR;
        }
    }
    /**
     * 更新播放、暂停和停止按钮
     */
    private void updatePausePlay() {
        if (mCurrentState == CURRENT_STATE_PAUSE || mCurrentState == CURRENT_STATE_IDLE || mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            query.id(R.id.media_play).image(R.drawable.m_play_ic_pause);
            query.id(R.id.media_play_icon).image(R.drawable.m_play_ic_pause);
            mHandler.removeMessages(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX);
        } else {
            query.id(R.id.media_play).image(R.drawable.m_play_ic_play);
            query.id(R.id.media_play_icon).image(R.drawable.m_play_ic_play);
            mHandler.sendEmptyMessageDelayed(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX, delayTimeToHideControlbox);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        isPortrait = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
        if (isPortrait) {
            hideTop();
            hideRight();
            hideVodTitle();
            hideSubtitles();
            hideExtraBox();
            hideFastRewindIcon();
            hideFastForwardIcon();
            showPlayButton();
            showFullScreenButton();
            hideBackButton();
            if (isInPlayingState()) {
                hidePlayFeedback();
                hidePlayIcon();

            }
        } else {
            showTop();
            showRight();
            showVodTitle();
            if (isTrailer) {
                hideExtraBox();
                hideResource();
            } else {
//                showSubtitles();
                showExtraBox();
//                showResource();
            }
            showFastRewindIcon();
            showFastForwardIcon();
            hidePlayButton();
            hideFullScreenButton();
            showBackButton();
            showPlayFeedback();
            if (isInPlayingState()) showPlayIcon();
        }
        setFullScreen(isPortrait);
    }
    /**
     * 设置界面方向
     */
    private void setFullScreen(boolean isPortrait) {
        if (mActivity != null) {

            WindowManager.LayoutParams attrs = mActivity.getWindow().getAttributes();
            if (!isPortrait) {
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                mActivity.getWindow().setAttributes(attrs);
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            } else {
                attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                mActivity.getWindow().setAttributes(attrs);
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        }
        doOnConfigurationChanged(isPortrait);
    }
    /**
     * 界面方向改变是刷新界面
     */
    private void doOnConfigurationChanged(final boolean portrait) {
        // change the bottom box height here
        if (portrait) {
            query.id(R.id.media_bottombox).height(initBottomBoxHeight, false);
            query.id(R.id.layout_episode).gone();
            mTopBox.setPadding(0, 0, 0, 0);
            mRightBox.setPadding(0, 0, 0, 0);
            mBottomBox.setPadding(0, 0, 0, 0);
            mSubtitlesView.setTextSize(VideoUtils.getTextSizeInPortrait(ContextProvider.getContext(), mCurrentSubSizePos));

        } else {
            query.id(R.id.media_bottombox).height(initBottomBoxHeight * 3 / 2, false);
            if (isSeries) {
                query.id(R.id.layout_episode).visible();
            }
            //全屏播放调整底部,右部和顶部布局的左右宽度
            mTopBox.setPadding(mContext.getResources().getDimensionPixelOffset(R.dimen.c_ui_sm_36), 0, mContext.getResources().getDimensionPixelOffset(R.dimen.c_ui_sm_36), 0);
            mRightBox.setPadding(mContext.getResources().getDimensionPixelOffset(R.dimen.c_ui_sm_36), 0, mContext.getResources().getDimensionPixelOffset(R.dimen.c_ui_sm_36), 0);
            mBottomBox.setPadding(mContext.getResources().getDimensionPixelOffset(R.dimen.c_ui_sm_36), 0, mContext.getResources().getDimensionPixelOffset(R.dimen.c_ui_sm_36), 0);
            mSubtitlesView.setTextSize(VideoUtils.getTextSizeInLandscape(ContextProvider.getContext(), mCurrentSubSizePos));
        }
    }

    public long syncProgress() {
        long position = getCurrentPositionWhenPlaying();
        long duration = getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mSeekBar.setProgress((int) pos);
            }
            int percent = getCurrentBufferPercentage();
            mSeekBar.setSecondaryProgress(percent * 10);
        }

        mCurrentTime.setText(generateTime(position));
        mEndTime.setText(generateTime(duration));
        if (isAllowDisplayNextEpisode && !isTrailer && isSeries && (duration > position) && (duration - position <= PLAY_NEXT_EPISODE_THRESHOLD_VALUE)) {
            showNextEpisode();
        } else {
            hideNextEpisode();
        }
        return position;
    }
    /**
     * 进度条滑动监听
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        logger.d( "onProgressChanged. progress:%d fromUser:%b", progress, fromUser);
        if (!fromUser) {
            /**不是用户拖动的，自动播放滑动的情况*/
            return;
        } else {
            long duration = getDuration();
            int position = (int) ((duration * progress * 1.0) / 1000);
            String time = generateTime(position);
            mCurrentTime.setText(time);
        }
    }

    /**停止拖动*/
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeMessages(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
        mHandler.removeMessages(VodMediaConstant.MESSAGE_HIDE_BOTTOM_AND_TOP_BOX);
    }

    /**停止拖动*/
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isManualSeeking = true;
        if (ConnectionChangeListener.ConnectionType.NONE == currentConnectionType) {
            if (onEventCallback != null) {
                onEventCallback.onNetworkDisconnected(new BaseVideoPlayer.Action<Boolean>() {
                    @Override
                    public void call(Boolean isCacheFilm) {
                        if (!isCacheFilm) {
                            onVideoPause();
                        } else {
                            long duration = getDuration();
                            seekTo((int) ((duration * seekBar.getProgress() * 1.0) / 1000));
                        }
                    }
                });
            }
        } else {
            long duration = getDuration();
            seekTo((int) ((duration * seekBar.getProgress() * 1.0) / 1000));
        }
        mHandler.removeMessages(VodMediaConstant.MESSAGE_UPDATE_PROGRESS);
        mHandler.sendEmptyMessageDelayed(VodMediaConstant.MESSAGE_UPDATE_PROGRESS, 1000);
    }

    /**========Activity生命周期方法回调==========*/
    public void onPause() {
        if (!isHandPause) {
            onVideoPause();
        }
    }

    public void onResume() {
        if (!isHandPause) {
            onVideoResume(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
    }
}
