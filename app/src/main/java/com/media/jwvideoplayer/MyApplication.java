package com.media.jwvideoplayer;

import android.app.Activity;
import android.content.res.Configuration;
import androidx.multidex.MultiDexApplication;

import com.media.jwvideoplayer.cache.VideoProxyCacheManager;
import com.media.jwvideoplayer.cache.utils.StorageUtils;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.lib.utils.AppContextUtils;

import java.io.File;
import java.util.Locale;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.onAdaptListener;
import me.jessyan.autosize.utils.ScreenUtils;

/**
 * Created by Joyce.wang on 2022/9/28.
 */
public class MyApplication extends MultiDexApplication {
    private static Logger logger = LoggerFactory.getLogger(MyApplication.class.getName());
    @Override
    public void onCreate() {
        super.onCreate();
        AppContextUtils.init(this);
        initAutoSizeConfig();
        initVideoProxyCache();
    }

    private void initAutoSizeConfig() {
        AutoSizeConfig.getInstance()
                //是否让框架支持自定义 Fragment 的适配参数, 由于这个需求是比较少见的, 所以须要使用者手动开启
                //如果没有这个需求建议不开启
                .setCustomFragment(true)
                .setDesignHeightInDp(640)
                .setDesignWidthInDp(360)
                //是否屏蔽系统字体大小对 AndroidAutoSize 的影响, 如果为 true, App 内的字体的大小将不会跟随系统设置中字体大小的改变
                //如果为 false, 则会跟随系统设置中字体大小的改变, 默认为 false
                .setExcludeFontScale(true)
                //屏幕适配监听器
                .setOnAdaptListener(new onAdaptListener() {
                    @Override
                    public void onAdaptBefore(Object target, Activity activity) {
                        //使用以下代码, 可支持 Android 的分屏或缩放模式, 但前提是在分屏或缩放模式下当用户改变您 App 的窗口大小时
                        //系统会重绘当前的页面, 经测试在某些机型, 某些情况下系统不会重绘当前页面, ScreenUtils.getScreenSize(activity) 的参数一定要不要传 Application!!!
//                        AutoSizeConfig.getInstance().setScreenWidth(ScreenUtils.getScreenSize(activity)[0]);
//                        AutoSizeConfig.getInstance().setScreenHeight(ScreenUtils.getScreenSize(activity)[1]);
                        //使用以下代码, 可支持 Android 的分屏或缩放模式, 但前提是在分屏或缩放模式下当用户改变您 App 的窗口大小时
                        //系统会重绘当前的页面, 经测试在某些机型, 某些情况下系统不会重绘当前页面, ScreenUtils.getScreenSize(activity) 的参数一定要不要传 Application!!!
                        AutoSizeConfig.getInstance().setScreenWidth(ScreenUtils.getScreenSize(activity)[0]);
                        AutoSizeConfig.getInstance().setScreenHeight(ScreenUtils.getScreenSize(activity)[1]);
                        logger.d(String.format(Locale.ENGLISH, "%s onAdaptBefore!", target.getClass().getName()));
                        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            //设置横屏设计尺寸
                            AutoSizeConfig.getInstance()
                                    .setDesignWidthInDp(640)
                                    .setDesignHeightInDp(360);
                        } else {
                            //设置竖屏设计尺寸
                            AutoSizeConfig.getInstance()
                                    .setDesignWidthInDp(360)
                                    .setDesignHeightInDp(640);
                        }
                    }

                    @Override
                    public void onAdaptAfter(Object target, Activity activity) {
                        logger.d(String.format(Locale.ENGLISH, "%s onAdaptAfter!", target.getClass().getName()));
                    }
                })

                //是否打印 AutoSize 的内部日志, 默认为 true, 如果您不想 AutoSize 打印日志, 则请设置为 false
                .setLog(false)
        //是否使用设备的实际尺寸做适配, 默认为 false, 如果设置为 false, 在以屏幕高度为基准进行适配时
        //AutoSize 会将屏幕总高度减去状态栏高度来做适配
        //设置为 true 则使用设备的实际屏幕高度, 不会减去状态栏高度
//                .setUseDeviceSize(true)

        //是否全局按照宽度进行等比例适配, 默认为 true, 如果设置为 false, AutoSize 会全局按照高度进行适配
//                .setBaseOnWidth(false)

        //设置屏幕适配逻辑策略类, 一般不用设置, 使用框架默认的就好
//                .setAutoAdaptStrategy(new AutoAdaptStrategy())
        ;
    }

    private void initVideoProxyCache() {
        File saveFile = StorageUtils.getVideoFileDir(this);
        if (!saveFile.exists()) {
            saveFile.mkdir();
        }

        logger.i("video cache file path: %s", saveFile.getAbsolutePath());
        VideoProxyCacheManager.Builder builder = new VideoProxyCacheManager.Builder().
                setFilePath(saveFile.getAbsolutePath());   //缓存存储位置
        VideoProxyCacheManager.getInstance().initProxyConfig(builder.build());
    }
}
