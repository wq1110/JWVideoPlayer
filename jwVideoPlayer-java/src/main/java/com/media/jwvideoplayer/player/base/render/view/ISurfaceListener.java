package com.media.jwvideoplayer.player.base.render.view;

import android.view.Surface;

/**
 * Created by Joyce.wang on 2023/3/16.
 */
public interface ISurfaceListener {
    void onSurfaceCreated(Surface surface);

    void onSurfaceChanged(Surface surface, int width, int height);

    boolean onSurfaceDestroyed(Surface surface);

    void onSurfaceUpdated(Surface surface);
}
