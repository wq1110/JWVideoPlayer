package com.media.jwvideoplayer.player.base.render.view;

import android.view.View;
import com.media.jwvideoplayer.player.base.render.MeasureHelper;

/**
 * Created by Joyce.wang on 2023/3/16.
 */
public interface IRenderView {
    ISurfaceListener getSurfaceListener();

    /**
     * Surface变化监听，必须
     */
    void setSurfaceListener(ISurfaceListener surfaceListener);

    /**
     * 当前view高度，必须
     */
    int getSizeH();

    /**
     * 当前view宽度，必须
     */
    int getSizeW();

    /**
     * 实现该接口的view，必须
     */
    View getRenderView();
    /**
     * 渲染view通过MeasureFormVideoParamsListener获取视频的相关参数，必须
     */
    void setVideoParamsListener(MeasureHelper.MeasureFormVideoParamsListener listener);
}
