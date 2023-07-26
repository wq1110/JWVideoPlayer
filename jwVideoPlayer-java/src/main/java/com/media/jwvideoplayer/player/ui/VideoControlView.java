package com.media.jwvideoplayer.player.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.R;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.lib.provider.ContextProvider;
import com.media.jwvideoplayer.lib.utils.ToastUtils;
import com.media.jwvideoplayer.player.VodMediaConstant;


/**
 * Created by Joyce.wang on 2023/3/16.
 * 播放UI的显示、控制层、手势处理等
 */
public abstract class VideoControlView extends VideoView implements View.OnClickListener, View.OnTouchListener {
    private static Logger logger = LoggerFactory.getLogger(VideoControlView.class.getName());
    //亮度
    protected View mLayoutBrightness;//亮度调整整体布局view
    protected TextView mBrightnessTv;//亮度百分比
    //调节声音
    protected View mLayoutVolume;//调节声音整体布局view
    protected ImageView mVolumeIv;//声音图片view
    protected TextView mVolumeTv;//声音调节百分比
    //快进快退
    protected View mLayoutFastForward;//快进快退整体布局view
    protected TextView mFastwardTv;//快进快退具体值（s）
    protected TextView mFastwardTargetTv;//快进快退target（在总时长中的位置）
    protected TextView mFastwardAllTv;//总时长展示view
    protected ImageView mOrienLock;//屏幕锁
    protected ImageView mFullScreen;

    protected boolean isProgressSlideEnable = true;
    protected boolean isVolumeSlideEnable = true;//volume sliding
    protected boolean isBrightnessSlideEnable = true;//brightness slide
    protected boolean isForbidTouch;//禁止触摸，默认可以触摸，true为禁止false为可触摸
    protected long newPosition = -1;//滑动进度条得到的新位置，和当前播放位置是有区别的,newPosition =0也会调用设置的，故初始化值为-1
    protected boolean mRotationLocked = false;//屏幕是否有上锁
    /**
     * Activity界面方向监听
     */
    protected OrientationEventListener orientationEventListener;
    /**
     * 音频管理器
     */
    protected AudioManager audioManager;
    /**
     * 当前声音大小
     */
    protected int volume;
    /**
     * 设备最大音量
     */
    protected int mMaxVolume;
    /**
     * 当前亮度大小
     */
    protected float brightness;
    protected GestureDetector gestureDetector;

    public VideoControlView(@NonNull Context context) {
        super(context);
    }

    public VideoControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VideoControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            onHandleMessage(msg);
        }
    };

    protected void onHandleMessage(Message msg) {
        switch (msg.what) {
            case VodMediaConstant.MESSAGE_HIDE_GESTURE_BOX:
                mLayoutVolume.setVisibility(GONE);
                mLayoutBrightness.setVisibility(GONE);
                mLayoutFastForward.setVisibility(GONE);
                break;
            /**滑动完成，设置播放进度*/
            case VodMediaConstant.MESSAGE_SEEK_NEW_POSITION:
                if (newPosition >= 0) {
                    seekTo((int) newPosition);
                    newPosition = -1;
                }
                break;
        }
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mLayoutBrightness = findViewById(R.id.app_video_brightness_box);
        mBrightnessTv = findViewById(R.id.app_video_brightness);

        mLayoutVolume = findViewById(R.id.app_video_volume_box);
        mVolumeIv = findViewById(R.id.app_video_volume_icon);
        mVolumeTv = findViewById(R.id.app_video_volume);

        mLayoutFastForward = findViewById(R.id.app_video_fastForward_box);
        mFastwardTv = findViewById(R.id.app_video_fastForward);
        mFastwardTargetTv = findViewById(R.id.app_video_fastForward_target);
        mFastwardAllTv = findViewById(R.id.app_video_fastForward_all);

        mOrienLock = findViewById(R.id.btn_orientation_lock);
        mFullScreen = findViewById(R.id.media_fullscreen);

        mOrienLock.setOnClickListener(this);
        mFullScreen.setOnClickListener(this);
        setClickable(true);//不设置为true，会导致GestureDetector监听不生效
        setOnTouchListener(this);

        gestureDetector = new GestureDetector(ContextProvider.getContext(), new PlayerGestureListener());

        orientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation > 10 && orientation < 170) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                } else if (orientation > 190 && orientation < 350) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        };
    }

    public void hideFullScreenButton() {
        this.mFullScreen.setVisibility(GONE);
    }

    public void showFullScreenButton() {
        this.mFullScreen.setVisibility(VISIBLE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.media_fullscreen == id) {
            toggleFullScreen();
        } else if (R.id.btn_orientation_lock == id) {
            toggleLockRotation();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
        }
        if (gestureDetector.onTouchEvent(motionEvent)) {
            return true;
        }
        // 处理手势结束
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                endGesture();
                break;
        }
        return false;
    }
    /**
     * 手势结束
     */
    private void endGesture() {
        isManualSeeking = true;
        volume = -1;
        brightness = -1f;
        mHandler.removeMessages(VodMediaConstant.MESSAGE_HIDE_GESTURE_BOX);
        mHandler.sendEmptyMessageDelayed(VodMediaConstant.MESSAGE_HIDE_GESTURE_BOX, 500);
        if (newPosition >= 0) {
            mHandler.removeMessages(VodMediaConstant.MESSAGE_SEEK_NEW_POSITION);
            mHandler.sendEmptyMessage(VodMediaConstant.MESSAGE_SEEK_NEW_POSITION);
        }
    }

    protected void enableProgressSlide(boolean enable) {
        this.isProgressSlideEnable = enable;
    }

    //是否禁止触摸
    public void forbidTouch(boolean forbidTouch) {
        this.isForbidTouch = forbidTouch;
    }

    //设置是否禁止滑动调节声音
    protected void enableVolumeSlide(boolean enable) {
        this.isVolumeSlideEnable = enable;
    }

    //设置是否禁止滑动调节亮度
    protected void enableBrightnessSlide(boolean enable) {
        this.isBrightnessSlideEnable = enable;
    }


    private class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
        /**
         * 是否是按下的标识，默认为其他动作，true为按下标识，false为其他动作
         */
        private boolean isDownTouch;
        /**
         * 是否声音控制,默认为亮度控制，true为声音控制，false为亮度控制
         */
        private boolean isVolume;
        /**
         * 是否横向滑动，默认为纵向滑动，true为横向滑动，false为纵向滑动
         */
        private boolean isLandscape;
        /**
         * 双击
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            /**视频视窗双击事件*/
            toggleFullScreen();
            return true;
        }
        /**
         * 按下
         */
        @Override
        public boolean onDown(MotionEvent e) {
            isDownTouch = true;
            return super.onDown(e);
        }
        /**
         * 滑动
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float mOldX = e1.getX(), mOldY = e1.getY();
            float deltaY = mOldY - e2.getY();
            float deltaX = mOldX - e2.getX();
            if (isDownTouch) {
                isLandscape = Math.abs(distanceX) >= Math.abs(distanceY);
                isVolume = mOldX > mScreenWidth * 0.5f;
                isDownTouch = false;
            }

            if (isLandscape) {
                /**进度设置*/
                onProgressSlide(-deltaX / getWidth());
                logger.d("onScroll horizonally");
            } else {
                if (!isForbidTouch) {
                    float percent = deltaY / getHeight();
                    if (isVolume) {
                        /**声音设置*/
                        onVolumeSlide(percent);
                        logger.d("left onScroll vertically %s", percent);
                    } else {
                        /**亮度设置*/
                        onBrightnessSlide(percent);
                        logger.d("right onScroll vertically %s", percent);
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
        /**
         * 单击
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            /**视频视窗单击事件*/
            onSingleTapUpEvent();
            return true;
        }
    }

    public void toggleFullScreen() {
        if (mRotationLocked) {
            ToastUtils.showToast(getContext(), R.string.m_play_str_screen_locked);
            return;
        }

        Activity mActivity = (Activity) getContext();
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            orientationEventListener.disable();
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            orientationEventListener.enable();
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    /**
     * 获取界面方向
     */
    public int getScreenOrientation() {
        Activity mActivity = (Activity) getContext();
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }
        return orientation;
    }

    //快进或者快退滑动改变进度
    private void onProgressSlide(float percent) {
        if (!isProgressSlideEnable) {
            return;
        }
        int position = getCurrentPositionWhenPlaying();
        long duration = getDuration();
        long deltaMax = Math.min(100 * 1000, duration - position);
        long delta = (long) (deltaMax * percent);
        newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition = 0;
            delta = -position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            mLayoutFastForward.setVisibility(VISIBLE);
            String text = showDelta > 0 ? ("+" + showDelta) : "" + showDelta;
            mFastwardTv.setText(text + "s");
            mFastwardTargetTv.setText(generateTime(newPosition) + "/");
            mFastwardAllTv.setText(generateTime(duration));
        }
    }

    //时长格式化显示
    protected String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    //滑动改变声音大小
    private void onVolumeSlide(float percent) {
        if (!isVolumeSlideEnable) {
            return;
        }
        if (volume == -1) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0)
                volume = 0;
        }
        int index = (int) (percent * mMaxVolume) + volume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;

        // 变更声音
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

        // 变更进度条
        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        if (i == 0) {
            s = "off";
        }
        // 显示
        mVolumeIv.setBackgroundResource(i == 0 ? R.drawable.m_play_ic_simple_player_volume_off_white_36dp : R.drawable.m_play_ic_simple_player_volume_up_white_36dp);
        mLayoutBrightness.setVisibility(GONE);
        mLayoutVolume.setVisibility(VISIBLE);
        mVolumeTv.setVisibility(VISIBLE);
    }

    //亮度滑动改变亮度
    private void onBrightnessSlide(float percent) {
        if (!isBrightnessSlideEnable) {
            return;
        }
        Activity mActivity = (Activity) mContext;
        if (brightness < 0) {
            brightness = mActivity.getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f) {
                brightness = 0.50f;
            } else if (brightness < 0.01f) {
                brightness = 0.01f;
            }
        }
        mLayoutBrightness.setVisibility(VISIBLE);
        WindowManager.LayoutParams lpa = mActivity.getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        mBrightnessTv.setText(((int) (lpa.screenBrightness * 100)) + "%");
        mActivity.getWindow().setAttributes(lpa);
    }

    /**
     * 锁定旋转方向
     */
    public void toggleLockRotation() {
        if (orientationEventListener == null) {
            return;
        }

        if (mRotationLocked) {
            mRotationLocked = false;
            orientationEventListener.enable();
            mOrienLock.setImageResource(R.drawable.m_play_ic_lock);
        } else {
            mRotationLocked = true;
            orientationEventListener.disable();
            mOrienLock.setImageResource(R.drawable.m_play_ic_locked);
        }
    }

    @Override
    protected void setStateAndUi(int state) {
        mCurrentState = state;
        Message message = mHandler.obtainMessage(VodMediaConstant.MESSAGE_CHANGE_PLAY_STATUS);
        message.arg1 = state;
        mHandler.sendMessage(message);
    }

    //处理视频窗口单击事件
    public abstract void onSingleTapUpEvent();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        if (orientationEventListener != null) orientationEventListener.disable();
    }
}
