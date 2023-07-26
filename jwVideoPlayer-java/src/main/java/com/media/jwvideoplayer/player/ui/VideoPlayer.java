package com.media.jwvideoplayer.player.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.R;
import com.media.jwvideoplayer.player.VodMediaConstant;
import com.media.jwvideoplayer.player.base.VideoManager;
import com.media.jwvideoplayer.player.base.listener.VideoViewBridge;

/**
 * Created by Joyce.wang on 2023/3/20.
 * 兼容的空View，目前用于 VideoManager的设置, 以及字幕功能
 */
public abstract class VideoPlayer extends BaseVideoPlayer {
    private static final String TAG = VideoPlayer.class.getSimpleName();

    public static final String KEY_SUBTITLE_TEXT = "key_subtitle_text";
    public static final String KEY_SUBTITLE_TEXT_SIZE = "key_subtitle_text_size";
    public static final String KEY_SUBTITLE_TEXT_COLOR = "key_subtitle_text_color";
    public static final String KEY_SUBTITLE_TEXT_BACKGROUND = "key_subtitle_text_background";
    public static final String KEY_IS_SHOW_SUBTITLE_VIEW = "key_is_show_subtitle_view";
    public static final String KEY_SUBTITLE_STAT = "key_subtitle_stat";

    public static final int TYPE_SUBTITLE_TEXT = 1;
    public static final int TYPE_SUBTITLE_TEXT_SIZE = 2;
    public static final int TYPE_SUBTITLE_TEXT_COLOR = 3;
    public static final int TYPE_SUBTITLE_TEXT_BACKGROUND = 4;

    public TextView mSubtitlesView;
    public View mSubtitles;

    public VideoPlayer(@NonNull Context context) {
        super(context);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(Context context) {
        super.init(context);

        mSubtitlesView = findViewById(R.id.media_subtitles_view);
        mSubtitles = findViewById(R.id.layout_subtitle);

        mSubtitles.setOnClickListener(this);
    }

    @Override
    protected void onHandleMessage(Message msg) {
        super.onHandleMessage(msg);
        switch (msg.what) {
            case VodMediaConstant.MESSAGE_UPDATE_SUBTITLE:
                if (msg.arg1 == TYPE_SUBTITLE_TEXT) {
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        String subtitleText = bundle.getString(KEY_SUBTITLE_TEXT);
                        boolean isShowSubtitleView = bundle.getBoolean(KEY_IS_SHOW_SUBTITLE_VIEW);
                        updateSubtitleText(isShowSubtitleView, subtitleText);
                    }
                } else if (msg.arg1 == TYPE_SUBTITLE_TEXT_SIZE){
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        float subtitleSize = bundle.getFloat(KEY_SUBTITLE_TEXT_SIZE);
                        mSubtitlesView.setTextSize(subtitleSize);
                    }
                } else if (msg.arg1 == TYPE_SUBTITLE_TEXT_COLOR) {
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        int subtitleColor = bundle.getInt(KEY_SUBTITLE_TEXT_COLOR);
                        mSubtitlesView.setTextColor(subtitleColor);
                    }
                } else if (msg.arg1 == TYPE_SUBTITLE_TEXT_BACKGROUND) {
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        int subtitleBackground = bundle.getInt(KEY_SUBTITLE_TEXT_BACKGROUND);
                        mSubtitlesView.setBackgroundColor(subtitleBackground);
                    }
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int id = v.getId();
        if (R.id.layout_subtitle == id) {
            switchSubtitles();
        }
    }

    private void switchSubtitles() {
        if (null != onEventCallback) {
            onEventCallback.onSubtitlesChange();
        }
    }

    @Override
    public VideoViewBridge getVideoManager() {
        return VideoManager.getInstance();
    }

    @Override
    protected void releaseVideos() {
        VideoManager.releaseAllVideos();
    }

    //隐藏字幕
    public void hideSubtitles() {
        this.mSubtitles.setVisibility(GONE);
    }

    //展示字幕
    public void showSubtitles() {
        this.mSubtitles.setVisibility(VISIBLE);
    }

    //更新字幕信息
    public void updateSubtitleText(boolean subtitleInShifting, Spanned spannedText) {
        //暂停缓冲时的播放进度会出现不稳定情况，导致字幕忽前忽后，所以在缓冲时停止字幕显示
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SUBTITLE_TEXT, spannedText != null ? spannedText.toString() : "");
        Message message = mHandler.obtainMessage(VodMediaConstant.MESSAGE_UPDATE_SUBTITLE);
        message.arg1 = TYPE_SUBTITLE_TEXT;
        if (!isPlaying() && !subtitleInShifting) {
            bundle.putBoolean(KEY_IS_SHOW_SUBTITLE_VIEW, false);
            message.setData(bundle);
            mHandler.sendMessage(message);
        } else {
            bundle.putBoolean(KEY_IS_SHOW_SUBTITLE_VIEW, true);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }
    }

    private void updateSubtitleText(boolean isShowSubtitleView, String subtitleTxt) {
        if (!isShowSubtitleView) {
            if (mSubtitlesView.getVisibility() == View.VISIBLE) {
                mSubtitlesView.setVisibility(View.GONE);
            }
        } else {
            if (mSubtitlesView.getVisibility() != View.VISIBLE) {
                mSubtitlesView.setVisibility(View.VISIBLE);
            }
        }
        if (mSubtitlesView.getVisibility() != View.VISIBLE) {
            return;
        }

        if (!TextUtils.isEmpty(subtitleTxt)) {
            setSubtitles(subtitleTxt);
        } else {
            setSubtitles("");
        }
    }
    public void setSubtitles(String text) {
        mSubtitlesView.setText(text);
    }

    public void toggleSubtitlesView(boolean show) {
        this.mSubtitlesView.setVisibility(show ? VISIBLE : GONE);
    }

    //设置字幕大小
    protected int mCurrentSubSizePos = 1;
    public void setSubtitlesSize(float size) {
        setSubtitlesSize(mCurrentSubSizePos, size);
    }

    public void setSubtitlesSize(int sizePos, float size) {
        mCurrentSubSizePos = sizePos;
        Bundle bundle = new Bundle();
        bundle.putFloat(KEY_SUBTITLE_TEXT_SIZE, size);
        Message message = mHandler.obtainMessage(VodMediaConstant.MESSAGE_UPDATE_SUBTITLE);
        message.arg1 = TYPE_SUBTITLE_TEXT_SIZE;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    //设置字幕颜色
    public void setSubtitlesTextColor(int textColor) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_SUBTITLE_TEXT_COLOR, textColor);
        Message message = mHandler.obtainMessage(VodMediaConstant.MESSAGE_UPDATE_SUBTITLE);
        message.arg1 = TYPE_SUBTITLE_TEXT_COLOR;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    //设置字幕背景颜色
    public void setSubtitlesBackgroundColor(int backColor) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_SUBTITLE_TEXT_BACKGROUND, backColor);
        Message message = mHandler.obtainMessage(VodMediaConstant.MESSAGE_UPDATE_SUBTITLE);
        message.arg1 = TYPE_SUBTITLE_TEXT_BACKGROUND;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }
//
//    //ass
//    public void initAss(byte[] content) {
//        if (assTask != null) assTask.initAss(mAssView, content);
//    }
//
//    public void updateAssTimeStamp(long timeStamp) {
//        if (assTask != null) assTask.updateAssTimeStamp(timeStamp);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (assTask != null) assTask.destory();
    }
}
