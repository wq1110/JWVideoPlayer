package com.media.jwvideoplayer.player.base.render.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.media.jwvideoplayer.player.base.render.MeasureHelper;
import com.media.jwvideoplayer.player.base.render.RenderView;

/**
 * Created by Joyce.wang on 2023/3/16.
 */
public class MediaSurfaceView extends SurfaceView implements SurfaceHolder.Callback, IRenderView, MeasureHelper.MeasureFormVideoParamsListener {
    private static final String TAG = MediaSurfaceView.class.getSimpleName();

    private ISurfaceListener iSurfaceListener;
    private MeasureHelper.MeasureFormVideoParamsListener videoParamsListener;
    private MeasureHelper measureHelper;

    public MediaSurfaceView(Context context) {
        super(context);
        init();
    }

    public MediaSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MediaSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MediaSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        measureHelper = new MeasureHelper(this, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureHelper.prepareMeasure(widthMeasureSpec, heightMeasureSpec, (int) getRotation());
        setMeasuredDimension(measureHelper.getMeasuredWidth(), measureHelper.getMeasuredHeight());
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        if (iSurfaceListener != null) {
            iSurfaceListener.onSurfaceCreated(surfaceHolder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        if (iSurfaceListener != null) {
            iSurfaceListener.onSurfaceChanged(surfaceHolder.getSurface(), width, height);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        //清空释放
        if (iSurfaceListener != null) {
            iSurfaceListener.onSurfaceDestroyed(surfaceHolder.getSurface());
        }
    }

    @Override
    public ISurfaceListener getSurfaceListener() {
        return this.iSurfaceListener;
    }

    @Override
    public void setSurfaceListener(ISurfaceListener surfaceListener) {
        getHolder().addCallback(this);
        this.iSurfaceListener = surfaceListener;
    }

    @Override
    public int getSizeH() {
        return getHeight();
    }

    @Override
    public int getSizeW() {
        return getWidth();
    }

    @Override
    public View getRenderView() {
        return this;
    }

    @Override
    public void setVideoParamsListener(MeasureHelper.MeasureFormVideoParamsListener listener) {
        this.videoParamsListener = listener;
    }

    @Override
    public int getCurrentVideoWidth() {
        if (videoParamsListener != null) {
            return videoParamsListener.getCurrentVideoWidth();
        }
        return 0;
    }

    @Override
    public int getCurrentVideoHeight() {
        if (videoParamsListener != null) {
            return videoParamsListener.getCurrentVideoHeight();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (videoParamsListener != null) {
            return videoParamsListener.getVideoSarNum();
        }
        return 0;
    }

    @Override
    public int getVideoSarDen() {
        if (videoParamsListener != null) {
            return videoParamsListener.getVideoSarDen();
        }
        return 0;
    }

    /**
     * 添加播放的view
     */
    public static MediaSurfaceView addSurfaceView(Context context, ViewGroup textureViewContainer, int rotate,
                                                final ISurfaceListener iSurfaceListener,
                                                final MeasureHelper.MeasureFormVideoParamsListener videoParamsListener) {
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }
        MediaSurfaceView showSurfaceView = new MediaSurfaceView(context);
        showSurfaceView.setSurfaceListener(iSurfaceListener);
        showSurfaceView.setVideoParamsListener(videoParamsListener);
        showSurfaceView.setRotation(rotate);
        RenderView.addToParent(textureViewContainer, showSurfaceView);
        return showSurfaceView;
    }
}
