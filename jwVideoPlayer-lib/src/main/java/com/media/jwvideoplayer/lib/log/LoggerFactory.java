package com.media.jwvideoplayer.lib.log;

/**
 * Created by Joyce.wang on 2022/9/20.
 */
public class LoggerFactory {
    public static Logger getLogger(Class clazz) {
        return getLogger(clazz.getCanonicalName());
    }

    public static Logger getLogger(String tag) {
        if (Logger.getLogger() == null)
            Logger.setLogger(new DefaultLogger());
        return Logger.newInstance(tag);
    }
}
