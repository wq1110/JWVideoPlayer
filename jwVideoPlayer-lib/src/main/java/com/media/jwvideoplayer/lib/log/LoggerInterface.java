package com.media.jwvideoplayer.lib.log;

/**
 * Created by Joyce.wang on 2022/9/20.
 */
public interface LoggerInterface {
    void log(int priority, String tag, String message, Throwable t);
}
