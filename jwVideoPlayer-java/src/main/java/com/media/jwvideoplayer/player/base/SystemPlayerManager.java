package com.media.jwvideoplayer.player.base;

import android.content.Context;
import android.media.AudioManager;
import android.media.PlaybackParams;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.lib.provider.ContextProvider;
import com.media.jwvideoplayer.player.base.model.IjkOptionModel;
import com.media.jwvideoplayer.player.base.model.VideoModel;

import java.io.File;
import java.util.List;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

/**
 * Created by Joyce.wang on 2023/3/15.
 * AndroidMediaPlayer manager
 */
public class SystemPlayerManager extends BasePlayerManager {
    private static Logger logger = LoggerFactory.getLogger(SystemPlayerManager.class.getName());
    private Context context;

    private AndroidMediaPlayer mediaPlayer;

    private Surface surface;

    private boolean release;

    private long lastTotalRxBytes = 0;

    private long lastTimeStamp = 0;

    private boolean isPlaying = false;

    @Override
    public IMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void initVideoPlayer(Context context, Message message, List<IjkOptionModel> optionModelList) {
        this.context = context.getApplicationContext();
        mediaPlayer = new AndroidMediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        release = false;
        VideoModel videoModel = (VideoModel) message.obj;
        try {
            //set url
            Uri uri = Uri.parse(videoModel.getUrl());
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
            mediaPlayer.setLooping(videoModel.isLooping());
            if (videoModel.getSpeed() != 1 && videoModel.getSpeed() > 0) {
                setSpeed(videoModel.getSpeed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mPlayerInitSuccessListener != null) mPlayerInitSuccessListener.onPlayerInitSuccess(getMediaPlayer(), videoModel);
    }

    @Override
    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
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
        if (msg.obj == null && mediaPlayer != null && !release) {
            mediaPlayer.setSurface(null);
        } else if (msg.obj != null) {
            Surface holder = (Surface) msg.obj;
            surface = holder;
            if (mediaPlayer != null && holder.isValid() && !release) {
                mediaPlayer.setSurface(holder);
            }
            if (!isPlaying) {
                pause();
            }
        }
    }

    @Override
    public void setNeedMute(boolean needMute) {
        try {
            if (mediaPlayer != null && !release) {
                if (needMute) {
                    mediaPlayer.setVolume(0, 0);
                } else {
                    mediaPlayer.setVolume(1, 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            release = true;
            mediaPlayer.release();
            mediaPlayer = null;
        }
        lastTotalRxBytes = 0;
        lastTimeStamp = 0;
    }

    @Override
    public int getBufferedPercentage() {
        return -1;
    }

    @Override
    public long getNetSpeed() {
        if (mediaPlayer != null) {
            return getNetSpeed(context);
        }
        return 0;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {

    }

    @Override
    public boolean isSurfaceSupportLockCanvas() {
        return false;
    }

    @Override
    public void setSpeed(float speed, boolean soundTouch) {
        setSpeed(speed);
    }

    private void setSpeed(float speed) {
        if (release) {
            return;
        }
        if (mediaPlayer != null && mediaPlayer.getInternalMediaPlayer() != null && mediaPlayer.isPlayable()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PlaybackParams playbackParams = new PlaybackParams();
                    playbackParams.setSpeed(speed);
                    mediaPlayer.getInternalMediaPlayer().setPlaybackParams(playbackParams);
                } else {
                    logger.e("not support setSpeed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private long getNetSpeed(Context context) {
        if (context == null) {
            return 0;
        }
        long nowTotalRxBytes = TrafficStats.getUidRxBytes(context.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);//转为KB
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = (nowTimeStamp - lastTimeStamp);
        if (calculationTime == 0) {
            return calculationTime;
        }
        //毫秒转换
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime);
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return speed;
    }
}
