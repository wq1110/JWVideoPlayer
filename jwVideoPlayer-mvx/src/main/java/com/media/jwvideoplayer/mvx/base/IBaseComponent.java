package com.media.jwvideoplayer.mvx.base;

import android.view.View;

import androidx.annotation.LayoutRes;

/**
 * 用户界面组件
 *
 * @Author Damon
 */
public interface IBaseComponent extends LayoutAsyncSupported {
    //布局文件ID
    @LayoutRes
    int getLayoutID();
    //配置UI以及其他事情
    void configUI(View view);
    //是否允许异步解析布局文件
    boolean allowInflateAsync();
}
