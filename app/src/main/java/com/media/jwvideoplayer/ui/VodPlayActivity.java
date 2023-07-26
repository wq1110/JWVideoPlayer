package com.media.jwvideoplayer.ui;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.media.jwvideoplayer.BR;
import com.media.jwvideoplayer.R;
import com.media.jwvideoplayer.databinding.LayoutActivityDetailBinding;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.mvx.mvvm.MVVMBaseActivity;
import com.media.jwvideoplayer.player.listener.ConnectionChangeListener;
import com.media.jwvideoplayer.player.ui.BaseVideoPlayer;
import com.media.jwvideoplayer.player.ui.VideoView;
import com.media.jwvideoplayer.viewmodel.VodPlayViewModel;

import me.jessyan.autosize.AutoSizeCompat;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Joyce.wang on 2022/10/14.
 */
public class VodPlayActivity extends MVVMBaseActivity<VodPlayViewModel, LayoutActivityDetailBinding>
        implements ViewTreeObserver.OnGlobalLayoutListener {
    private static Logger logger = LoggerFactory.getLogger(VodPlayActivity.class.getName());

    // layoutParams for screen rotation operation
    private RelativeLayout.LayoutParams layoutParamsForPortrait;
    private RelativeLayout.LayoutParams layoutParamsForLandscape;
    private int screenWidthInPixel;

    private ImmersionBar mImmersionBar;//状态栏沉浸

    @Override
    public int getLayoutID() {
        return R.layout.layout_activity_detail;
    }

    @Override
    public Resources getResources() {
        Resources resources = super.getResources();
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                AutoSizeCompat.autoConvertDensity(resources, 360, true);
            } else {
                //非主线程需要切换到主线程，否者autoConvertDensity会crash.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AutoSizeCompat.autoConvertDensity(resources, 360, true);
                    }
                });
            }
        } else {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                AutoSizeCompat.autoConvertDensity(resources, 640, true);
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AutoSizeCompat.autoConvertDensity(resources, 640, true);
                    }
                });
            }
        }
        return super.getResources();
    }

    @Override
    protected void autoSizeFix(Resources resources) {

    }

    @Override
    protected int getBindingVariable() {
        return BR.vodPlay;
    }

    @Override
    protected void initData() {
        initView();
    }

    private void initView() {
        //init VodMediaPlayer
        mBinding.vodMediaPlayer.hideLoadingBox();
        mBinding.vodMediaPlayer.hideTop();
        mBinding.vodMediaPlayer.hideBottom();
        mBinding.vodMediaPlayer.showPlayIcon();
        mBinding.vodMediaPlayer.hideSubtitles();
        mBinding.vodMediaPlayer.forbidTouch(true);
        mBinding.vodMediaPlayer.setAllowDisplayNextEpisode(true);
        mBinding.vodMediaPlayer.setOnEventCallback(playerEventCallback);

        //init layoutPlayer layout params
        screenWidthInPixel = getResources().getDisplayMetrics().widthPixels;
        layoutParamsForPortrait = (RelativeLayout.LayoutParams) mBinding.layoutPlayer.getLayoutParams();
        layoutParamsForPortrait.height = screenWidthInPixel * 9 / 16;//DensityUtil.px2dip(this, (float) screenWidthInPixel * 9 / 16);
        mBinding.layoutPlayer.setLayoutParams(layoutParamsForPortrait);
        mBinding.layoutPlayer.requestLayout();
        layoutParamsForLandscape = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mBinding.layoutVodDetail.setVisibility(View.VISIBLE);

        //初始化沉浸式状态栏
        if (isStatusBarEnabled()) {
            statusBarConfig().init();
        }
    }

    private void resetPlayer() {
        if (mBinding.vodMediaPlayer != null) {
            mBinding.vodMediaPlayer.release();
            mBinding.vodMediaPlayer.hideLoadingBox();
            mBinding.vodMediaPlayer.hideTop();
            mBinding.vodMediaPlayer.hideBottom();
            mBinding.vodMediaPlayer.showPlayIcon();
            mBinding.vodMediaPlayer.hideFastForwardIcon();
            mBinding.vodMediaPlayer.hideFastRewindIcon();
            mBinding.vodMediaPlayer.hideRight();
            mBinding.vodMediaPlayer.setTrailer(false);
            mBinding.vodMediaPlayer.setSubtitles("");
            mBinding.vodMediaPlayer.setAllowDisplayNextEpisode(true);
            mBinding.vodMediaPlayer.setOnEventCallback(playerEventCallback);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            logger.i("it is portrait");
            mBinding.layoutVodDetail.setVisibility(View.VISIBLE);
            mBinding.layoutPlayer.setLayoutParams(layoutParamsForPortrait);
            mBinding.vodMediaPlayer.forbidTouch(true);
            showSystemUI();
            showTitleBar();
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            logger.i("it is landscape");
            mBinding.layoutVodDetail.setVisibility(View.GONE);//横屏隐藏的原因是为了解决横竖切换导致字体和ui改变
            mBinding.layoutPlayer.setLayoutParams(layoutParamsForLandscape);
            mBinding.vodMediaPlayer.forbidTouch(false);
            hideSystemUI();
            hideTitleBar();
        }
        mBinding.vodMediaPlayer.onConfigurationChanged(newConfig);
    }

    /**
     * 是否使用沉浸式状态栏
     */
    public boolean isStatusBarEnabled() {
        return true;
    }

    /**
     * 获取状态栏沉浸的配置对象
     */
    public ImmersionBar getStatusBarConfig() {
        return mImmersionBar;
    }

    /**
     * 初始化沉浸式状态栏
     */
    private ImmersionBar statusBarConfig() {
        //在BaseActivity里初始化
        mImmersionBar = ImmersionBar.with(this)
//                .statusBarDarkFont(statusBarDarkFont())    //默认状态栏字体颜色为黑色
                .keyboardEnable(false, WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);  //解决软键盘与底部输入框冲突问题，默认为false，还有一个重载方法，可以指定软键盘mode
        //必须设置View树布局变化监听，否则软键盘无法顶上去，还有模式必须是SOFT_INPUT_ADJUST_PAN
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        return mImmersionBar;
    }

    //显示虚拟按键
    private void showSystemUI() {
        getStatusBarConfig().fullScreen(false).hideBar(BarHide.FLAG_SHOW_BAR).init();
    }

    //隐藏虚拟按键
    private void hideSystemUI() {
        getStatusBarConfig().fullScreen(true).hideBar(BarHide.FLAG_HIDE_BAR).init();
    }

    private boolean isPlayerLandscape() {
        if (mBinding.vodMediaPlayer.getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || mBinding.vodMediaPlayer.getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            return true;
        }
        return false;
    }

    private void showTitleBar() {
        if (mBinding.layoutTitleBar.getVisibility() == View.GONE) {
            mBinding.layoutTitleBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideTitleBar() {
        if (mBinding.layoutTitleBar.getVisibility() == View.VISIBLE) {
            mBinding.layoutTitleBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onGlobalLayout() {

    }

    private void autoPlay() {
        String url = "http://vjs.zencdn.net/v/oceans.mp4";
        if (isPlayerLandscape()) {
            hideSystemUI();
        }
        mBinding.vodMediaPlayer.setAllowDisplayNextEpisode(true);
        mBinding.vodMediaPlayer.setVideoUrl(url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding.vodMediaPlayer.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    }

    private VideoView.OnVodMediaPlayerListener playerEventCallback = new VideoView.OnVodMediaPlayerListener() {
        @Override
        public void onPreparePlay() {
            autoPlay();
        }

        @Override
        public void onNetworkChange(BaseVideoPlayer.Action action) {

        }

        @Override
        public void onNetworkChange(ConnectionChangeListener.ConnectionType type) {

        }

        @Override
        public void onNetworkDisconnected(BaseVideoPlayer.Action<Boolean> action) {

        }

        @Override
        public void onResourcesChange() {

        }

        @Override
        public void onSubtitlesChange() {

        }

        @Override
        public void onVideoEpisodeChange() {

        }

        @Override
        public void onNextEpisode() {
            resetPlayer();
        }

        @Override
        public void onCompletion(IMediaPlayer iMediaPlayer) {
            mBinding.vodMediaPlayer.showPlayIcon();
            mBinding.vodMediaPlayer.hideBottom();
            mBinding.vodMediaPlayer.showTop();
            resetPlayer();
        }

        @Override
        public void onSeekCompleted(long duration, long position) {

        }

        @Override
        public void onError(IMediaPlayer iMediaPlayer, int position, int reason) {

        }

        @Override
        public void onClickTitleHelp() {

        }

        @Override
        public void onPrepared(IMediaPlayer mp) {

        }

        @Override
        public void onPlayerType(int playerType) {

        }

        @Override
        public void onLocaleChange() {
            finish();
        }

        @Override
        public void finishActivity() {
            finish();
        }

        @Override
        public void onBufferingStart(IMediaPlayer mp) {

        }

        @Override
        public void onBufferingEnd(IMediaPlayer mp, boolean isPrepare, long bufferingStartTime, long bufferingTime, long prepareFirstBufferingTime, long seekFirstBuffingTime, long startBufferingPosition) {

        }
    };
}
