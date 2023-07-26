package com.media.jwvideoplayer.player.base.render;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.media.jwvideoplayer.player.base.render.view.IRenderView;
import com.media.jwvideoplayer.player.base.render.view.ISurfaceListener;
import com.media.jwvideoplayer.player.base.render.view.MediaSurfaceView;
import com.media.jwvideoplayer.player.base.render.view.MediaTextureView;

/**
 * Created by Joyce.wang on 2023/3/16.
 * render绘制中间控件
 */
public class RenderView {
    protected IRenderView mRenderView;
    /*************************RenderView function start *************************/
    public void requestLayout() {
        if (mRenderView != null) {
            mRenderView.getRenderView().requestLayout();
        }
    }

    public float getRotation() {
        return mRenderView.getRenderView().getRotation();
    }

    public void setRotation(float rotation) {
        if (mRenderView != null)
            mRenderView.getRenderView().setRotation(rotation);
    }

    public void invalidate() {
        if (mRenderView != null)
            mRenderView.getRenderView().invalidate();
    }

    public int getWidth() {
        return (mRenderView != null) ? mRenderView.getRenderView().getWidth() : 0;
    }

    public int getHeight() {
        return (mRenderView != null) ? mRenderView.getRenderView().getHeight() : 0;
    }

    public View getRenderView() {
        if (mRenderView != null)
            return mRenderView.getRenderView();
        return null;
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return mRenderView.getRenderView().getLayoutParams();
    }

    public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if (mRenderView != null)
            mRenderView.getRenderView().setLayoutParams(layoutParams);
    }

    /**
     * 添加播放的view
     */
    public void addView(final Context context, final ViewGroup textureViewContainer, final int rotate,
                        final ISurfaceListener gsySurfaceListener,
                        final MeasureHelper.MeasureFormVideoParamsListener videoParamsListener) {
        if (VideoType.getRenderType() == VideoType.SUFRACE) {
            mRenderView = MediaSurfaceView.addSurfaceView(context, textureViewContainer, rotate, gsySurfaceListener, videoParamsListener);
        } else {
            mRenderView = MediaTextureView.addTextureView(context, textureViewContainer, rotate, gsySurfaceListener, videoParamsListener);
        }
    }

    /*************************common function *************************/

    public static void addToParent(ViewGroup textureViewContainer, View render) {
        int params = getTextureParams();
        if (textureViewContainer instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(params, params);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            textureViewContainer.addView(render, layoutParams);
        } else if (textureViewContainer instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(params, params);
            layoutParams.gravity = Gravity.CENTER;
            textureViewContainer.addView(render, layoutParams);
        }
    }

    /**
     * 获取布局参数
     *
     * @return
     */
    public static int getTextureParams() {
        boolean typeChanged = (VideoType.getShowType() != VideoType.SCREEN_TYPE_DEFAULT);
        return (typeChanged) ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
    }
}
