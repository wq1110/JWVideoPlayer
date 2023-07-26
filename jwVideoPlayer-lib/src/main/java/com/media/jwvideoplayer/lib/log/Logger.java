package com.media.jwvideoplayer.lib.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joyce.wang on 2022/9/20.
 */
public class Logger {

    public static final int NONE = 1;

    public static final int VERBOSE = 2;

    public static final int DEBUG = 3;

    public static final int INFO = 4;

    public static final int WARN = 5;

    public static final int ERROR = 6;

    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    private static LoggerInterface listener;

    private static int LOG_LEVEL = DEBUG;

    private String tag;

    public static Logger newInstance(String tag) {
        return new Logger(tag);
    }

    public Logger(String tag) {
        this.tag = tag;
    }

    public static void setLogger(LoggerInterface logger) {
        listener = logger;
    }

    public static LoggerInterface getLogger() {
        return listener;
    }

    public static void logLevel(int level) {
        if (level < NONE || level > ERROR)
            throw new IllegalArgumentException("log level should between NONE-2 and ERROR-6");
        LOG_LEVEL = level;
    }

    private void log(int priority, String tag, String message, Throwable t) {
        if (listener != null) listener.log(priority, tag, message, t);
    }

    public void v(String message, Object... args) {
        prepareLog(VERBOSE, null, message, args);
    }

    public void v(Throwable t, String message, Object... args) {
        prepareLog(VERBOSE, t, message, args);
    }

    public void d(String message, Object... args) {
        prepareLog(DEBUG, null, message, args);
    }

    public void d(Throwable t, String message, Object... args) {
        prepareLog(DEBUG, t, message, args);
    }

    public boolean isDebug() {
        return LOG_LEVEL <= DEBUG;
    }
    public boolean isInfo() {
        return LOG_LEVEL <= INFO;
    }

    public void i(String message, Object... args) {
        prepareLog(INFO, null, message, args);
    }

    public void i(Throwable t, String message, Object... args) {
        prepareLog(INFO, t, message, args);
    }


    public void w(String message, Object... args) {
        prepareLog(WARN, null, message, args);
    }

    public void w(Throwable t, String message, Object... args) {
        prepareLog(WARN, t, message, args);
    }

    public void e(String message, Object... args) {
        prepareLog(ERROR, null, message, args);
    }

    public void e(Throwable t, String message, Object... args) {
        prepareLog(ERROR, t, message, args);
    }


    private void prepareLog(int priority, Throwable t, String message, Object... args) {
        try {
            if (listener != null && isLoggable(priority)) {
                if (message != null && message.length() == 0) {
                    message = null;
                }

                if (message == null) {
                    if (t == null) {
                        return;
                    }

                    message = getStackTraceString(t);
                } else {
                    if (args.length > 0) {
                        message = String.format(message, args);
                    }

                    if (t != null) {
                        message = message + "\n" + getStackTraceString(t);
                    }
                }

                log(priority, getTag(), message, t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isLoggable(int priority) {
        return priority >= LOG_LEVEL;
    }

    private String getStackTraceString(Throwable tr) {
        if (tr == null) return "";
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException)
                return "";
            t = t.getCause();
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private String getTag() {
        if (tag != null && tag.length() > 0) {
            return tag;
        }
        StackTraceElement[] stackTrace = (new Throwable()).getStackTrace();
        if (stackTrace.length <= 5) {
            return "";
        } else {
            String tag = stackTrace[5].getClassName();
            Matcher m = ANONYMOUS_CLASS.matcher(tag);
            if (m.find()) {
                tag = m.replaceAll("");
            }
            return tag.substring(tag.lastIndexOf(46) + 1);
        }
    }
}