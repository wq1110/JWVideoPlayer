package com.media.jwvideoplayer.player.base.render;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.player.base.render.view.ISurfaceListener;

/**
 * Created by Joyce.wang on 2023/3/16.
 */
public abstract class MediaTextureRenderView extends FrameLayout implements ISurfaceListener, MeasureHelper.MeasureFormVideoParamsListener {
    private static Logger logger = LoggerFactory.getLogger(MediaTextureRenderView.class.getName());

    //native绘制
    protected Surface mSurface;

    //渲染控件
    protected RenderView mTextureView;

    //渲染控件父类
    protected ViewGroup mTextureViewContainer;

    //画面选择角度
    protected int mRotate;

    public MediaTextureRenderView(@NonNull Context context) {
        super(context);
    }

    public MediaTextureRenderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaTextureRenderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MediaTextureRenderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onSurfaceCreated(Surface surface) {
        mSurface = surface;
        setDisplay(mSurface);
        onSurfaceCreatedEvent(surface);
    }

    @Override
    public void onSurfaceChanged(Surface surface, int width, int height) {
        onSurfaceChangedEvent(surface, width, height);
    }

    @Override
    public boolean onSurfaceDestroyed(Surface surface) {
        //清空释放
        setDisplay(null);
        //同一消息队列中去release
        onSurfaceDestroyedEvent(surface);
        return true;
    }

    @Override
    public void onSurfaceUpdated(Surface surface) {

    }

    /**
     * 添加播放的view
     * 继承后重载addTextureView，继承GSYRenderView后实现自己的RenderView类，既可以使用自己自定义的显示层
     */
    protected void addTextureView() {
        mTextureView = new RenderView();
        mTextureView.addView(getContext(), mTextureViewContainer, mRotate, this, this);
    }

    /**
     * 获取布局参数
     *
     * @return
     */
    protected int getTextureParams() {
        boolean typeChanged = (VideoType.getShowType() != VideoType.SCREEN_TYPE_DEFAULT);
        return (typeChanged) ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
    }

    /**
     * 调整TextureView去适应比例变化
     */
    protected void changeTextureViewShowType() {
        if (mTextureView != null) {
            int params = getTextureParams();
            ViewGroup.LayoutParams layoutParams = mTextureView.getLayoutParams();
            layoutParams.width = params;
            layoutParams.height = params;
            mTextureView.setLayoutParams(layoutParams);
        }
    }

    /**
     * 获取渲染的代理层
     */
    public RenderView getRenderProxy() {
        return mTextureView;
    }

    //设置播放
    protected abstract void setDisplay(Surface surface);

    protected abstract void onSurfaceCreatedEvent(Surface surface);

    protected abstract void onSurfaceChangedEvent(Surface surface, int width, int height);
    //释放
    protected abstract void onSurfaceDestroyedEvent(Surface surface);
}
