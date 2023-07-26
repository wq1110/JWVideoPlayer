package com.media.jwvideoplayer;

import androidx.multidex.MultiDexApplication;
import com.media.jwvideoplayer.lib.utils.AppContextUtils;

/**
 * Created by Joyce.wang on 2022/9/28.
 */
public class MyApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContextUtils.init(this);
    }
}
