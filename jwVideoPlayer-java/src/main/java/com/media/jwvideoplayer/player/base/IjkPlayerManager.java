package com.media.jwvideoplayer.player.base;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import com.media.jwvideoplayer.BuildConfig;
import com.media.jwvideoplayer.lib.preferences.CommonPreference;
import com.media.jwvideoplayer.lib.provider.ContextProvider;
import com.media.jwvideoplayer.player.base.model.IjkOptionModel;
import com.media.jwvideoplayer.player.base.model.VideoModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkLibLoader;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;

/**
 * Created by Joyce.wang on 2023/3/15.
 * Ijkplayer manager
 */
public class IjkPlayerManager extends BasePlayerManager {
    //audiotrack 阻塞自动恢复配置
    public static final String ENABLE_AUTO_RESTART_AUDIO_KEY = "enable_auto_restart_audio_key";

    private int logLevel = IjkMediaPlayer.IJK_LOG_INFO;

    private IjkMediaPlayer mediaPlayer;

    private IjkLibLoader ijkLibLoader;
    private List<IjkOptionModel> optionModelList;

    private Surface surface;

    /**
     * 使用编解码器硬编码还是软编码，true 硬编码 false 为软编码
     */
    private boolean usingMediaCodec = true;
    private boolean usingOpenSLES = false;

    @Override
    public IMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void initVideoPlayer(Context context, Message message, List<IjkOptionModel> optionModelList) {
        mediaPlayer = (ijkLibLoader == null) ? new IjkMediaPlayer() : new IjkMediaPlayer(ijkLibLoader);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //设置ijk错误日志级别
        if (BuildConfig.DEBUG) {
            IjkMediaPlayer.native_setLogLevel(logLevel);
        } else {
            IjkMediaPlayer.native_setLogLevel(logLevel);
        }
        mediaPlayer.setOnNativeInvokeListener(new IjkMediaPlayer.OnNativeInvokeListener() {
            @Override
            public boolean onNativeInvoke(int i, Bundle bundle) {
                return true;
            }
        });

        VideoModel videoModel = (VideoModel) message.obj;
        String url = videoModel.getUrl();

        //设置ijk option相关参数
        try {
            if (usingMediaCodec) {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", 1);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-all-videos", 1);
            } else {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 0);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", 0);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-all-videos", 0);
            }
            if (usingOpenSLES) {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
            } else {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
            }

            String pixelFormat = "";
            if (TextUtils.isEmpty(pixelFormat)) {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
            } else {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
            }

            if (CommonPreference.getInstance().getBoolean(ENABLE_AUTO_RESTART_AUDIO_KEY, true)) {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-auto-restart-audio", 1);
            } else {
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-auto-restart-audio", 0);
            }

            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
//                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);

            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
            //参考： https://ffmpeg.org/ffmpeg-protocols.html
            // ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect_at_eof", 1);
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect_streamed", 1);
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-naked-csd", 1);

            //SeekTo设置优化
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);

            mediaPlayer.setLooping(videoModel.isLooping());
            if (videoModel.getSpeed() != 1 && videoModel.getSpeed() > 0) {
                mediaPlayer.setSpeed(videoModel.getSpeed());
            }

            //set url
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (TextUtils.isEmpty(scheme) || scheme.equalsIgnoreCase("file"))) {
                IMediaDataSource dataSource = new FileMediaDataSource(new File(uri.toString()));
                mediaPlayer.setDataSource(dataSource);
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mediaPlayer.setDataSource(ContextProvider.getContext(), uri, videoModel.getHeaders());
            } else {
                mediaPlayer.setDataSource(uri.toString());
            }

            if (mPlayerInitSuccessListener != null) mPlayerInitSuccessListener.onPlayerInitSuccess(getMediaPlayer(), videoModel);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public int getVideoWidth() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoHeight();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void seekTo(long time) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(time);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoSarNum();
        }
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoSarDen();
        }
        return 1;
    }

    @Override
    public void showDisplay(Message msg) {
        if (msg.obj == null && mediaPlayer != null) {
            mediaPlayer.setSurface(null);
        } else {
            Surface holder = (Surface) msg.obj;
            surface = holder;
            if (mediaPlayer != null && holder.isValid()) {
                mediaPlayer.setSurface(holder);
            }
        }
    }

    @Override
    public void setNeedMute(boolean needMute) {
        if (mediaPlayer != null) {
            if (needMute) {
                mediaPlayer.setVolume(0, 0);
            } else {
                mediaPlayer.setVolume(1, 1);
            }
        }
    }

    @Override
    public void setVolume(float left, float right) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(left, right);
        }
    }

    @Override
    public void releaseSurface() {
        if (surface != null) {
            //surface.release();
            surface = null;
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public int getBufferedPercentage() {
        return -1;
    }

    @Override
    public long getNetSpeed() {
        if (mediaPlayer != null) {
            return mediaPlayer.getTcpSpeed();
        }
        return 0;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {
        if (mediaPlayer != null) {
            mediaPlayer.setSpeed(speed);
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", (soundTouch) ? 1 : 0);
        }
    }

    @Override
    public boolean isSurfaceSupportLockCanvas() {
        return true;
    }

    @Override
    public void setSpeed(float speed, boolean soundTouch) {
        if (speed > 0) {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.setSpeed(speed);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (soundTouch) {
                IjkOptionModel ijkOptionModel =
                        new IjkOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);
                List<IjkOptionModel> list = getOptionModelList();
                if (list != null) {
                    list.add(ijkOptionModel);
                } else {
                    list = new ArrayList<>();
                    list.add(ijkOptionModel);
                }
                setOptionModelList(list);
            }

        }
    }

    public IjkTrackInfo[] getTrackInfo() {
        if (mediaPlayer != null) {
            return mediaPlayer.getTrackInfo();
        }
        return null;
    }

    public int getSelectedTrack(int trackType) {
        if (mediaPlayer != null) {
            return mediaPlayer.getSelectedTrack(trackType);
        }
        return -1;
    }

    public void selectTrack(int track) {
        if (mediaPlayer != null) {
            mediaPlayer.selectTrack(track);
        }
    }

    public void deselectTrack(int track) {
        if (mediaPlayer != null) {
            mediaPlayer.deselectTrack(track);
        }
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public IjkLibLoader getIjkLibLoader() {
        return ijkLibLoader;
    }

    public void setIjkLibLoader(IjkLibLoader ijkLibLoader) {
        this.ijkLibLoader = ijkLibLoader;
    }

    public List<IjkOptionModel> getOptionModelList() {
        return optionModelList;
    }

    public void setOptionModelList(List<IjkOptionModel> optionModelList) {
        this.optionModelList = optionModelList;
    }

    private void initIJKOption(IjkMediaPlayer ijkMediaPlayer, List<IjkOptionModel> optionModelList) {
        if (optionModelList != null && optionModelList.size() > 0) {
            for (IjkOptionModel ijkOptionModel : optionModelList) {
                if (ijkOptionModel.getValueType() == IjkOptionModel.VALUE_TYPE_INT) {
                    ijkMediaPlayer.setOption(ijkOptionModel.getCategory(),
                            ijkOptionModel.getName(), ijkOptionModel.getValueInt());
                } else {
                    ijkMediaPlayer.setOption(ijkOptionModel.getCategory(),
                            ijkOptionModel.getName(), ijkOptionModel.getValueString());
                }
            }
        }
    }
}
