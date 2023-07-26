package com.media.jwvideoplayer.player.base;

import com.media.jwvideoplayer.player.base.listener.IPlayerInitSuccessListener;
import com.media.jwvideoplayer.player.base.model.VideoModel;

/**
 * Created by Joyce.wang on 2023/3/15.
 */
public abstract class BasePlayerManager implements IPlayerManager {
    protected IPlayerInitSuccessListener mPlayerInitSuccessListener;

    public IPlayerInitSuccessListener getPlayerPreparedSuccessListener() {
        return mPlayerInitSuccessListener;
    }

    public void setPlayerInitSuccessListener(IPlayerInitSuccessListener listener) {
        this.mPlayerInitSuccessListener = listener;
    }

    protected void initSuccess(VideoModel videoModel) {
        if (mPlayerInitSuccessListener != null) {
            mPlayerInitSuccessListener.onPlayerInitSuccess(getMediaPlayer(), videoModel);
        }
    }
}
