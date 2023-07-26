package com.media.jwvideoplayer.mvx.base;

public interface LayoutAsyncSupported {
    //组件的View是否已经准备好
    boolean viewReady();
    //View准备好为前提 do sth
    void doAfterViewReady(Runnable runnable);
}
