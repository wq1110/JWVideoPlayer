package com.media.jwvideoplayer.player.base.listener;

import com.media.jwvideoplayer.player.base.model.VideoModel;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Joyce.wang on 2023/3/15.
 */
public interface IPlayerInitSuccessListener {
    void onPlayerInitSuccess(IMediaPlayer player, VideoModel model);
}
