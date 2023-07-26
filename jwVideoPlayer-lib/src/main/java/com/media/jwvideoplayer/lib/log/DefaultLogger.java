package com.media.jwvideoplayer.lib.log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Joyce.wang on 2022/9/20.
 */
public class DefaultLogger implements LoggerInterface {
    @Override
    public void log(int priority, String tag, String message, Throwable t) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String level = "N";
        switch (priority) {
            case 2:
                level = "V";
                break;
            case 3:
                level = "D";
                break;
            case 4:
                level = "I";
                break;
            case 5:
                level = "W";
                break;
            case 6:
                level = "E";
                break;
        }
        System.out.println("[" + sdf.format(new Date()) + "]" + "[" + level + "]" + "[" + tag + "] " + message);
        if (t != null)
            t.printStackTrace();
    }
}
