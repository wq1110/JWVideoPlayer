package com.media.jwvideoplayer.mvx.async;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Printer;

import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;
import com.media.jwvideoplayer.mvx.appstat.AppManager;
import com.media.jwvideoplayer.mvx.appstat.features.IAppFeatures;
import com.media.jwvideoplayer.mvx.appstat.features.IAppForeground;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.functions.Consumer;

/**
 * 提供默认的执行线程
 * 提供新的执行线程
 * 线程的管理: 池化
 */
public class ThreadsBox {
    private static Logger logger = LoggerFactory.getLogger(ThreadsBox.class.getName());
    public static final String DEBUG_THREAD_NAME = "debug_thread";

    /**
     * unite defaultHandlerThread for lightweight work,
     * if you have heavy work checking, you can create a new thread
     */
    private static volatile HandlerThread defaultHandlerThread;
    private static volatile Handler defaultHandler;
    private static volatile Handler defaultMainHandler = new Handler(Looper.getMainLooper());
    private static HashSet<HandlerThread> handlerThreads = new HashSet<>();
    public static boolean isDebug = false;


    public static Handler getDefaultMainHandler() {
        return defaultMainHandler;
    }

    public static HandlerThread getDefaultHandlerThread() {
        synchronized (ThreadsBox.class) {
            if (null == defaultHandlerThread) {
                defaultHandlerThread = new HandlerThread(DEBUG_THREAD_NAME);
                defaultHandlerThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
                defaultHandlerThread.start();
                defaultHandler = new Handler(defaultHandlerThread.getLooper());
                defaultHandlerThread.getLooper().setMessageLogging(isDebug ? new LooperPrinter() : null);
            }
            return defaultHandlerThread;
        }
    }

    public static Handler getDefaultHandler() {
        if (defaultHandler == null) {
            getDefaultHandlerThread();
        }
        return defaultHandler;
    }

    public static synchronized HandlerThread getNewHandlerThread(String name) {
        for (Iterator<HandlerThread> i = handlerThreads.iterator(); i.hasNext(); ) {
            HandlerThread element = i.next();
            if (!element.isAlive()) {
                i.remove();
            }
        }
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handlerThread.getLooper().setMessageLogging(isDebug ? new LooperPrinter() : null);
        handlerThreads.add(handlerThread);
        return handlerThread;
    }

    private static final class LooperPrinter implements Printer, IAppForeground {

        private ConcurrentHashMap<String, Info> hashMap = new ConcurrentHashMap<>();
        private boolean isForeground;

        LooperPrinter() {
            AppManager.INSTANCE.getApp().appStatChanged().subscribe(new Consumer<IAppFeatures.AppStat>() {
                @Override
                public void accept(IAppFeatures.AppStat appStat) throws Exception {
                    onForeground(appStat == IAppFeatures.AppStat.Foreground);
                }
            });
            this.isForeground = AppManager.INSTANCE.getApp().isAppForeground();
        }

        @Override
        public void println(String x) {
            if (isForeground) {
                return;
            }
            if (x.charAt(0) == '>') {
                int start = x.indexOf("} ");
                int end = x.indexOf("@", start);
                if (start < 0 || end < 0) {
                    return;
                }
                String content = x.substring(start, end);
                Info info = hashMap.get(content);
                if (info == null) {
                    info = new Info();
                    info.key = content;
                    hashMap.put(content, info);
                }
                ++info.count;
            }
        }

        @Override
        public void onForeground(boolean isForeground) {
            this.isForeground = isForeground;
            if (isForeground) {
                long start = System.currentTimeMillis();
                LinkedList<Info> list = new LinkedList<>();
                for (Info info : hashMap.values()) {
                    if (info.count > 1) {
                        list.add(info);
                    }
                }
                Collections.sort(list, new Comparator<Info>() {
                    @Override
                    public int compare(Info o1, Info o2) {
                        return o2.count - o1.count;
                    }
                });
                hashMap.clear();
                if (!list.isEmpty()) {
                    logger.i(String.format("matrix default thread has exec in background! %s cost:%s", list, System.currentTimeMillis() - start));
                }
            } else {
                hashMap.clear();
            }
        }

        class Info {
            String key;
            int count;

            @Override
            public String toString() {
                return key + ":" + count;
            }
        }

    }
}