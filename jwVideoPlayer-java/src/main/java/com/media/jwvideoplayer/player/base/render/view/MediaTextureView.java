package com.media.jwvideoplayer.player.base.render.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.player.base.render.MeasureHelper;
import com.media.jwvideoplayer.player.base.render.RenderView;
import com.media.jwvideoplayer.player.base.render.VideoType;

/**
 * Created by Joyce.wang on 2023/3/16.
 */
public class MediaTextureView extends TextureView implements TextureView.SurfaceTextureListener, IRenderView, MeasureHelper.MeasureFormVideoParamsListener {
    private static final String TAG = MediaTextureView.class.getSimpleName();
    private ISurfaceListener iSurfaceListener;

    private MeasureHelper.MeasureFormVideoParamsListener videoParamsListener;

    private MeasureHelper measureHelper;

    private SurfaceTexture mSaveTexture;
    private Surface mSurface;

    public MediaTextureView(@NonNull Context context) {
        super(context);
        init();
    }

    public MediaTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MediaTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MediaTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int i, int i1) {
        if (VideoType.isMediaCodecTexture()) {
            if (mSaveTexture == null) {
                mSaveTexture = surface;
                mSurface = new Surface(surface);
            } else {
                setSurfaceTexture(mSaveTexture);
            }
            if (iSurfaceListener != null) {
                iSurfaceListener.onSurfaceCreated(mSurface);
            }
        } else {
            mSurface = new Surface(surface);
            if (iSurfaceListener != null) {
                iSurfaceListener.onSurfaceCreated(mSurface);
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        if (iSurfaceListener != null) {
            iSurfaceListener.onSurfaceChanged(mSurface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        //清空释放
        if (iSurfaceListener != null) {
            iSurfaceListener.onSurfaceDestroyed(mSurface);
        }
        if (VideoType.isMediaCodecTexture()) {
            return (mSaveTexture == null);
        } else {
            return true;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
        //如果播放的是暂停全屏了
        if (iSurfaceListener != null) {
            iSurfaceListener.onSurfaceUpdated(mSurface);
        }
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

    @Override
    public ISurfaceListener getSurfaceListener() {
        return this.iSurfaceListener;
    }

    @Override
    public void setSurfaceListener(ISurfaceListener surfaceListener) {
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

    /**
     * 添加播放的view
     */
    public static MediaTextureView addTextureView(Context context, ViewGroup textureViewContainer, int rotate,
                                                final ISurfaceListener iSurfaceListener,
                                                final MeasureHelper.MeasureFormVideoParamsListener videoParamsListener) {
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }
        MediaTextureView mediaTextureView = new MediaTextureView(context);
        mediaTextureView.setSurfaceListener(iSurfaceListener);
        mediaTextureView.setVideoParamsListener(videoParamsListener);
        mediaTextureView.setRotation(rotate);
        RenderView.addToParent(textureViewContainer, mediaTextureView);
        return mediaTextureView;
    }
}
