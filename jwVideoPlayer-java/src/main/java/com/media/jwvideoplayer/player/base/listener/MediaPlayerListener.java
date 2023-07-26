package com.media.jwvideoplayer.player.base.listener;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Joyce.wang on 2023/3/15.
 */
public interface MediaPlayerListener {
    void onPrepared(IMediaPlayer mp);

    void onAutoCompletion();

    void onCompletion();

    void onBufferingUpdate(IMediaPlayer mp, int percent);

    void onSeekComplete(IMediaPlayer mp);

    void onError(IMediaPlayer mp, int what, int extra);

    void onInfo(IMediaPlayer mp, int what, int extra);

    void onVideoSizeChanged();

    void onVideoPause();

    void onVideoResume();

    void onVideoResume(boolean seek);
}
