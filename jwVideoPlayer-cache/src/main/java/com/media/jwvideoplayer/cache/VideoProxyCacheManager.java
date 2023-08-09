package com.media.jwvideoplayer.cache;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.media.jwvideoplayer.cache.common.ProxyMessage;
import com.media.jwvideoplayer.cache.common.VideoCacheConfig;
import com.media.jwvideoplayer.cache.common.VideoCacheException;
import com.media.jwvideoplayer.cache.common.VideoType;
import com.media.jwvideoplayer.cache.listener.IVideoCacheListener;
import com.media.jwvideoplayer.cache.listener.IVideoCacheTaskListener;
import com.media.jwvideoplayer.cache.listener.IVideoInfoParsedListener;
import com.media.jwvideoplayer.cache.listener.VideoInfoParsedListener;
import com.media.jwvideoplayer.cache.m3u8.M3U8;
import com.media.jwvideoplayer.cache.model.VideoCacheInfo;
import com.media.jwvideoplayer.cache.okhttp.IHttpPipelineListener;
import com.media.jwvideoplayer.cache.okhttp.NetworkConfig;
import com.media.jwvideoplayer.cache.okhttp.OkHttpManager;
import com.media.jwvideoplayer.cache.proxy.LocalProxyVideoServer;
import com.media.jwvideoplayer.cache.task.M3U8CacheTask;
import com.media.jwvideoplayer.cache.task.Mp4CacheTask;
import com.media.jwvideoplayer.cache.task.VideoCacheTask;
import com.media.jwvideoplayer.cache.utils.ProxyCacheUtils;
import com.media.jwvideoplayer.cache.utils.StorageUtils;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by Joyce.wang on 2023/4/7.
 */
public class VideoProxyCacheManager {
    private static Logger logger = LoggerFactory.getLogger(VideoProxyCacheManager.class.getName());
    private static volatile VideoProxyCacheManager sInstance = null;
    private ProxyMessageHandler mProxyHandler;

    private Map<String, VideoCacheTask> mCacheTaskMap = new ConcurrentHashMap<>();//cache task容器
    private Map<String, VideoCacheInfo> mCacheInfoMap = new ConcurrentHashMap<>();
    private Map<String, IVideoCacheListener> mCacheListenerMap = new ConcurrentHashMap<>();
    private Map<String, Long> mVideoSeekMd5PositionMap = new ConcurrentHashMap<>();      //发生seek的时候加入set, 如果可以播放了, remove掉
    private final Object mSeekPositionLock = new Object();

    private Set<String> mM3U8LocalProxyMd5Set = new ConcurrentSkipListSet<>();
    private Set<String> mM3U8LiveMd5Set = new ConcurrentSkipListSet<>();

    private String mPlayingUrlMd5;   //设置当前正在播放的视频url的MD5值

    public static VideoProxyCacheManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoProxyCacheManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoProxyCacheManager();
                }
            }
        }
        return sInstance;
    }

    private VideoProxyCacheManager() {
        HandlerThread handlerThread = new HandlerThread("proxy cache thread");
        handlerThread.start();
        mProxyHandler = new ProxyMessageHandler(handlerThread.getLooper());
    }

    private class ProxyMessageHandler extends Handler {

        public ProxyMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            VideoCacheInfo cacheInfo = (VideoCacheInfo) msg.obj;
            if (cacheInfo != null) {
                IVideoCacheListener cacheListener = mCacheListenerMap.get(cacheInfo.getSourceId());
                if (cacheListener != null) {
                    switch (msg.what) {
                        case ProxyMessage.MSG_VIDEO_PROXY_ERROR:
                            cacheListener.onCacheError(cacheInfo, 0);
                            break;
                        case ProxyMessage.MSG_VIDEO_PROXY_FORBIDDEN:
                            cacheListener.onCacheForbidden(cacheInfo);
                            break;
                        case ProxyMessage.MSG_VIDEO_PROXY_START:
                            cacheListener.onCacheStart(cacheInfo);
                            break;
                        case ProxyMessage.MSG_VIDEO_PROXY_PROGRESS:
                            cacheListener.onCacheProgress(cacheInfo);
                            break;
                        case ProxyMessage.MSG_VIDEO_PROXY_COMPLETED:
                            cacheListener.onCacheFinished(cacheInfo);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    //构建代理缓存的属性
    public static class Builder {

        private long mExpireTime = 7 * 24 * 60 * 60 * 1000;//缓存文件有效期
        private long mMaxCacheSize = 200 * 1024 * 1024;//缓存文件最大上限
        private String mFilePath;//缓存存储位置
        private int mReadTimeOut = 60 * 1000;//网络连接超时
        private int mConnTimeOut = 60 * 1000;//网络读超时
        private boolean mIgnoreCert;//是否忽略证书校验
        private int mPort;//本地代理的端口
        private boolean mUseOkHttp;//是否使用okhttp网络请求

        public Builder setExpireTime(long expireTime) {
            mExpireTime = expireTime;
            return this;
        }

        public Builder setMaxCacheSize(long maxCacheSize) {
            mMaxCacheSize = maxCacheSize;
            return this;
        }

        public Builder setFilePath(String filePath) {
            mFilePath = filePath;
            return this;
        }

        public Builder setReadTimeOut(int readTimeOut) {
            mReadTimeOut = readTimeOut;
            return this;
        }

        public Builder setConnTimeOut(int connTimeOut) {
            mConnTimeOut = connTimeOut;
            return this;
        }

        public Builder setIgnoreCert(boolean ignoreCert) {
            mIgnoreCert = ignoreCert;
            return this;
        }

        //需要自定义端口号的可以调用这个函数
        public Builder setPort(int port) {
            mPort = port;
            return this;
        }

        public Builder setUseOkHttp(boolean useOkHttp) {
            mUseOkHttp = useOkHttp;
            return this;
        }

        public VideoCacheConfig build() {
            return new VideoCacheConfig(mExpireTime, mMaxCacheSize, mFilePath, mReadTimeOut, mConnTimeOut, mIgnoreCert, mPort, mUseOkHttp);
        }
    }

    public void initProxyConfig(@NonNull VideoCacheConfig config) {
        ProxyCacheUtils.setVideoCacheConfig(config);
        new LocalProxyVideoServer();  //初始化本地代理服务

        NetworkConfig networkConfig = new NetworkConfig(config.getReadTimeOut(), config.getConnTimeOut(), config.ignoreCert());
        OkHttpManager.getInstance().initConfig(networkConfig, mHttpPipelineListener);

        //设置缓存清理规则
        StorageManager.getInstance().initCacheConfig(config.getFilePath(), config.getMaxCacheSize(), config.getExpireTime());
    }

    public void addCacheListener(String sourceId, String videoUrl, @NonNull IVideoCacheListener listener) {
        mCacheListenerMap.put(sourceId, listener);
    }

    public void removeCacheListener(String sourceId) {
        mCacheListenerMap.remove(sourceId);
    }

    public void releaseProxyReleases(String sourceId) {
        String md5 = ProxyCacheUtils.computeMD5(sourceId);
        removeCacheListener(sourceId);

        if (!TextUtils.isEmpty(md5)) {
            releaseProxyCacheSet(md5);
            removeVideoSeekInfo(md5);
        }
    }

    public String getTrailerCacheUrl(String sourceId) {
        String md5 = ProxyCacheUtils.computeMD5(sourceId);

        if (!TextUtils.isEmpty(md5)) {
            File saveDir = new File(ProxyCacheUtils.getConfig().getFilePath(), md5);
            if (saveDir.exists()) {
                VideoCacheInfo videoCacheInfo = StorageUtils.readVideoCacheInfo(saveDir);
                if (videoCacheInfo != null) {
                    return videoCacheInfo.getVideoUrl();
                }
            }
        }
        return null;
    }

    /**
     * @param sourceId
     * @param videoUrl  视频url
     */
    public void startRequestVideoInfo(String sourceId, String videoUrl) {
        startRequestVideoInfo(sourceId, videoUrl, new HashMap<>());
    }

    /**
     * @param sourceId
     * @param videoUrl 视频url
     * @param headers  请求的头部信息
     */
    public void startRequestVideoInfo(String sourceId, String videoUrl, Map<String, String> headers) {
        startRequestVideoInfo(sourceId, videoUrl, headers, new HashMap<>());
    }

    /**
     * @param sourceId
     * @param videoUrl    视频url
     * @param headers     请求的头部信息
     * @param extraParams 额外参数，这个map很有用，例如我已经知道当前请求视频的类型和长度，都可以在extraParams中设置,
     *                    详情见VideoParams
     */
    public void startRequestVideoInfo(String sourceId, String videoUrl, Map<String, String> headers, Map<String, Object> extraParams) {
        StorageManager.getInstance().initCacheInfo();

        String md5 = ProxyCacheUtils.computeMD5(sourceId);
        File saveDir = new File(ProxyCacheUtils.getConfig().getFilePath(), md5);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }
        VideoCacheInfo videoCacheInfo = StorageUtils.readVideoCacheInfo(saveDir);
        if (videoCacheInfo == null) {
            //之前没有缓存信息
            videoCacheInfo = new VideoCacheInfo(sourceId, videoUrl);
            videoCacheInfo.setMd5(md5);
            videoCacheInfo.setSavePath(saveDir.getAbsolutePath());

            final Object lock = VideoLockManager.getInstance().getLock(md5);
            final String finalMd5 = md5;

            VideoInfoParseManager.getInstance().parseVideoInfo(videoCacheInfo, headers, extraParams, new IVideoInfoParsedListener() {
                @Override
                public void onM3U8ParsedFinished(M3U8 m3u8, VideoCacheInfo cacheInfo) {
                    notifyLocalProxyLock(lock);
                    mM3U8LocalProxyMd5Set.add(finalMd5);
                    //开始发起请求M3U8视频中的ts数据
                    startM3U8Task(m3u8, cacheInfo, headers);
                }

                @Override
                public void onM3U8ParsedFailed(VideoCacheException videoCacheException, VideoCacheInfo cacheInfo) {
                    notifyLocalProxyLock(lock);
                    mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_ERROR, cacheInfo).sendToTarget();
                }

                @Override
                public void onM3U8LiveCallback(VideoCacheInfo cacheInfo) {
                    notifyLocalProxyLock(lock);
                    mM3U8LiveMd5Set.add(finalMd5);
                    mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_FORBIDDEN, cacheInfo).sendToTarget();
                }

                @Override
                public void onNonM3U8ParsedFinished(VideoCacheInfo cacheInfo) {
                    notifyLocalProxyLock(lock);
                    //开始发起请求视频数据
                    startNonM3U8Task(cacheInfo, headers);
                }

                @Override
                public void onNonM3U8ParsedFailed(VideoCacheException e, VideoCacheInfo cacheInfo) {
                    notifyLocalProxyLock(lock);
                    mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_ERROR, cacheInfo).sendToTarget();
                }
            });
        } else {
            if (videoCacheInfo.getVideoType() == VideoType.M3U8_TYPE) {
                //说明视频类型是M3U8类型
                final Object lock = VideoLockManager.getInstance().getLock(md5);
                final String finalMd5 = md5;
                VideoInfoParseManager.getInstance().parseProxyM3U8Info(videoCacheInfo, headers, new VideoInfoParsedListener() {
                    @Override
                    public void onM3U8ParsedFinished(M3U8 m3u8, VideoCacheInfo cacheInfo) {
                        notifyLocalProxyLock(lock);
                        mM3U8LocalProxyMd5Set.add(finalMd5);
                        //开始发起请求M3U8视频中的ts数据
                        startM3U8Task(m3u8, cacheInfo, headers);
                    }

                    @Override
                    public void onM3U8ParsedFailed(VideoCacheException e, VideoCacheInfo cacheInfo) {
                        notifyLocalProxyLock(lock);
                        mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_ERROR, cacheInfo).sendToTarget();
                    }
                });
            } else if (videoCacheInfo.getVideoType() == VideoType.M3U8_LIVE_TYPE) {
                //说明是直播
                mM3U8LiveMd5Set.add(md5);
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_FORBIDDEN, videoCacheInfo).sendToTarget();
            } else {
                startNonM3U8Task(videoCacheInfo, headers);
            }
        }
    }

    /**
     * 开始缓存M3U8任务
     * @param m3u8
     * @param cacheInfo
     * @param headers
     */
    private void startM3U8Task(M3U8 m3u8, VideoCacheInfo cacheInfo, Map<String, String> headers) {
        if (cacheInfo != null) {
            VideoCacheTask cacheTask = mCacheTaskMap.get(cacheInfo.getSourceId());
            if (cacheTask == null) {
                cacheTask = new M3U8CacheTask(cacheInfo, headers, m3u8);
                mCacheTaskMap.put(cacheInfo.getSourceId(), cacheTask);
            }
            startVideoCacheTask(cacheTask, cacheInfo);
        }
    }

    /**
     * 开始缓存非M3U8任务
     * @param cacheInfo
     * @param headers
     */
    private void startNonM3U8Task(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        if (cacheInfo != null) {
            VideoCacheTask cacheTask = mCacheTaskMap.get(cacheInfo.getSourceId());
            if (cacheTask == null) {
                //创建mp4缓存任务
                cacheTask = new Mp4CacheTask(cacheInfo, headers);
                mCacheTaskMap.put(cacheInfo.getSourceId(), cacheTask);
            }
            startVideoCacheTask(cacheTask, cacheInfo);
        }
    }

    private void startVideoCacheTask(VideoCacheTask cacheTask, VideoCacheInfo cacheInfo) {
        final Object lock = VideoLockManager.getInstance().getLock(cacheInfo.getMd5());
        cacheTask.setTaskListener(new IVideoCacheTaskListener() {
            @Override
            public void onTaskStart() {
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_START, cacheInfo).sendToTarget();
            }

            @Override
            public void onTaskProgress(float percent, long cachedSize, float speed) {
                logger.d("onTaskProgress percent: %s, cachedSize: %s, speed: %s", percent, cachedSize, speed);
                if (shouldNotifyLock(cacheInfo.getVideoType(), cacheInfo.getSourceId(), cacheInfo.getVideoUrl(), cacheInfo.getMd5())) {
                    notifyLocalProxyLock(lock);
                }
                cacheInfo.setPercent(percent);
                cacheInfo.setCachedSize(cachedSize);
                cacheInfo.setSpeed(speed);
                mCacheInfoMap.put(cacheInfo.getSourceId(), cacheInfo);
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_PROGRESS, cacheInfo).sendToTarget();
            }

            @Override
            public void onM3U8TaskProgress(float percent, long cachedSize, float speed, Map<Integer, Long> tsLengthMap) {
                notifyLocalProxyLock(lock);
                cacheInfo.setPercent(percent);
                cacheInfo.setCachedSize(cachedSize);
                cacheInfo.setSpeed(speed);
                cacheInfo.setTsLengthMap(tsLengthMap);
                mCacheInfoMap.put(cacheInfo.getSourceId(), cacheInfo);
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_PROGRESS, cacheInfo).sendToTarget();
            }

            @Override
            public void onTaskFailed(Exception e) {
                notifyLocalProxyLock(lock);
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_ERROR, cacheInfo).sendToTarget();
            }

            @Override
            public void onVideoSeekComplete() {
                notifyLocalProxyLock(lock);
            }

            @Override
            public void onTaskCompleted(long totalSize) {
                if (shouldNotifyLock(cacheInfo.getVideoType(), cacheInfo.getSourceId(), cacheInfo.getVideoUrl(), cacheInfo.getMd5())) {
                    logger.i("onTaskCompleted ----, totalSize="+totalSize);
                    notifyLocalProxyLock(lock);
                }
                cacheInfo.setTotalSize(totalSize);
                mCacheInfoMap.put(cacheInfo.getSourceId(), cacheInfo);
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_COMPLETED, cacheInfo).sendToTarget();
            }

            @Override
            public void onM3U8TaskCompleted(long totalSize) {
                if (shouldNotifyLock(cacheInfo.getVideoType(), cacheInfo.getSourceId(), cacheInfo.getVideoUrl(), cacheInfo.getMd5())) {
                    logger.i("onTaskCompleted ----, totalSize="+totalSize);
                    notifyLocalProxyLock(lock);
                }
                cacheInfo.setTotalSize(totalSize);
                mCacheInfoMap.put(cacheInfo.getSourceId(), cacheInfo);
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_COMPLETED, cacheInfo).sendToTarget();
            }
        });

        //开始缓存任务
        cacheTask.startCacheTask();
    }

    /**
     * 暂停缓存任务, 一般是主线程操作
     * @param sourceId
     */
    public void pauseCacheTask(String sourceId) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            cacheTask.pauseCacheTask();
        }
    }

    /**
     * 停止缓存任务, 一般是主线程操作
     * @param sourceId
     */
    public void stopCacheTask(String sourceId) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            cacheTask.stopCacheTask();
            mCacheTaskMap.remove(sourceId);
        }
    }

    /**
     * 恢复缓存任务,一般是主线程操作
     * @param sourceId
     */
    public void resumeCacheTask(String sourceId) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            cacheTask.resumeCacheTask();
        }
    }

    /**
     * 拖动播放进度条之后的操作
     * 纯粹客户端的操作, 一般是主线程操作
     * @param sourceId
     * @param percent
     */
    public void seekToCacheTaskFromClient(String sourceId, float percent) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            //当前seek到什么position在客户端不知道
            addVideoSeekInfoForM3U8(sourceId);
            cacheTask.seekToCacheTaskFromClient(percent);
        }
    }

    //针对非m3u8视频
    private void addVideoSeekInfo(String sourceId) {
        String md5 = ProxyCacheUtils.computeMD5(sourceId);
        synchronized (mSeekPositionLock) {
            logger.i("addVideoSeekInfo md5=" + md5 + ", sourceId=" + sourceId);
            mVideoSeekMd5PositionMap.put(md5, -1L);
        }
    }


    //针对m3u8视频
    private void addVideoSeekInfoForM3U8(String sourceId) {
        String md5 = ProxyCacheUtils.computeMD5(sourceId);
        synchronized (mSeekPositionLock) {
            logger.i("addVideoSeekInfoForM3U8 md5=" + md5 + ", sourceId=" + sourceId);
            mVideoSeekMd5PositionMap.put(md5, -1L);
        }
    }

    private boolean shouldNotifyLock(int videoType, String sourceId, String url, String md5) {
        synchronized (mSeekPositionLock) {
            //只有非M3U8视频才能进入这个逻辑
            if (videoType == VideoType.OTHER_TYPE && mVideoSeekMd5PositionMap.containsKey(md5)) {
                long position = mVideoSeekMd5PositionMap.get(md5).longValue();
                logger.i("shouldNotifyLock position=" + position + ", url=" + url);
                if (position > 0) {
                    boolean isMp4PositionSegExisted = isMp4PositionSegExisted(sourceId, url, position);
                    logger.i("shouldNotifyLock position=" + position + ", isMp4PositionSegExisted=" + isMp4PositionSegExisted);
                    if (isMp4PositionSegExisted) {
                        mVideoSeekMd5PositionMap.remove(md5);
                        return true;
                    } else {
                        //说明发生了seek, 但是seek请求并没有结束
                        return false;
                    }
                } else {
                    if (isMp4Completed(sourceId, url)) {
                        mVideoSeekMd5PositionMap.remove(md5);
                        return true;
                    }
                    //说明次数有seek操作,但是seek操作还没有从local server端发送过来
                    return false;
                }
            }
            return true;
        }
    }

    private void removeVideoSeekInfo(String md5) {
        synchronized (mSeekPositionLock) {
            if (mVideoSeekMd5PositionMap.containsKey(md5)) {
                logger.i("removeVideoSeekSet = " + md5);
                mVideoSeekMd5PositionMap.remove(md5);
            }
        }
    }

    /**
     * 服务端调用到客户端的通知, 这儿可以精确确定客户端应该从什么地方开始seek
     *
     * 从服务端调用过来, 肯定不是主线程, 所以要切换到主线程
     *
     * 这是针对非M3U8视频的
     * @param sourceId
     * @param url
     * @param startPosition
     */
    public void seekToCacheTaskFromServer(String sourceId, String url, long startPosition) {
        String md5 = ProxyCacheUtils.computeMD5(sourceId);
        boolean shouldSeek = false;
        synchronized (mSeekPositionLock) {
            long oldPosition = mVideoSeekMd5PositionMap.containsKey(md5) ? mVideoSeekMd5PositionMap.get(md5) : 0L;
            //说明这是一个新的seek操作, oldPosition =0L, 说明此时没有发生seek操作
            if (oldPosition == -1L) {
                logger.i("setVideoRangeRequest startPosition=" + startPosition);
                mVideoSeekMd5PositionMap.put(md5, startPosition);
                shouldSeek = true;
            }
        }

        final boolean seekByServer = shouldSeek;
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null && seekByServer) {
            cacheTask.seekToCacheTaskFromServer(startPosition);
        }
//        VideoProxyThreadUtils.runOnUiThread(() -> {
//            VideoCacheTask cacheTask = mCacheTaskMap.get(finalSourceId);
//            if (cacheTask != null && seekByServer) {
//                cacheTask.seekToCacheTaskFromServer(startPosition);
//            }
//        });
    }

    /**
     * 针对M3U8视频,从服务端传入分片索引到客户端来
     * @param url
     * @param segIndex
     */
    public void seekToCacheTaskFromServerForM3u8(String sourceId, String url, int segIndex) {
        String md5 = ProxyCacheUtils.computeMD5(sourceId);
        boolean shouldSeek = false;
        synchronized (mSeekPositionLock) {
            if (mVideoSeekMd5PositionMap.containsKey(md5)) {
                mVideoSeekMd5PositionMap.remove(md5);
                shouldSeek = true;
            }
        }
        final boolean seekByServer = shouldSeek;
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null && seekByServer) {
            cacheTask.seekToCacheTaskFromServer(segIndex);
        }
//        VideoProxyThreadUtils.runOnUiThread(() -> {
//            VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
//            if (cacheTask != null && seekByServer) {
//                cacheTask.seekToCacheTaskFromServer(segIndex);
//            }
//        });
    }

    /**
     * 当前MP4视频是否已经缓存到了startPosition位置
     * @param url
     * @param startPosition
     * @return
     */
    public boolean isMp4PositionSegExisted(String sourceId, String url, long startPosition) {
        if (startPosition == -1L) {
            //说明也没有seek 操作
            return true;
        }
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            return cacheTask.isMp4PositionSegExisted(startPosition);
        }
        return true;
    }

    /**
     * 当前MP4文件是否下载完全
     * @param sourceId
     * @param url
     */
    public boolean isMp4Completed(String sourceId, String url) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            return cacheTask.isMp4Completed();
        }
        return false;
    }

    /**
     * 从position开始,之后的数据都缓存完全了.
     * @param sourceId
     * @param url
     * @param position
     */
    public boolean isMp4CompletedFromPosition(String sourceId, String url, long position) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            return cacheTask.isMp4CompletedFromPosition(position);
        }
        return false;
    }

    /**
     * 当前position数据是否可以write到socket中
     * @param sourceId
     * @param url
     * @param position
     * @return
     */
    public boolean shouldWriteResponseData(String sourceId, String url, long position) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            return cacheTask.isMp4PositionSegExisted(position);
        }
        return false;
    }

    public long getMp4CachedPosition(String sourceId, String url, long position) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(sourceId);
        if (cacheTask != null) {
            return cacheTask.getMp4CachedPosition(position);
        }
        return 0L;
    }

    private void notifyLocalProxyLock(Object lock) {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * 当前proxy m3u8是否生成
     * @param md5
     * @return
     */
    public boolean isM3U8LocalProxyReady(String md5) {
        return mM3U8LocalProxyMd5Set.contains(md5);
    }

    /**
     * 是否是直播类型
     * @param md5
     * @return
     */
    public boolean isM3U8LiveType(String md5) {
        return mM3U8LiveMd5Set.contains(md5);
    }

    public void releaseProxyCacheSet(String md5) {
        mM3U8LiveMd5Set.remove(md5);
        mM3U8LocalProxyMd5Set.remove(md5);
    }

    public void setPlayingUrlMd5(String md5) {
        mPlayingUrlMd5 = md5;
    }

    public String getPlayingUrlMd5() {
        return mPlayingUrlMd5;
    }

    public boolean isM3U8SegCompleted(String m3u8Md5, int tsIndex, String filePath) {
        if (TextUtils.isEmpty(m3u8Md5) || TextUtils.isEmpty(filePath)) {
            return false;
        }
        File segFile = new File(filePath);
        if (!segFile.exists() || segFile.length() == 0) {
            return false;
        }
        for(Map.Entry entry : mCacheInfoMap.entrySet()) {
            String url = String.valueOf(entry.getKey());
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            VideoCacheInfo cacheInfo = mCacheInfoMap.get(url);
            if (cacheInfo != null && TextUtils.equals(cacheInfo.getMd5(), m3u8Md5)) {
                Map<Integer, Long> tsLengthMap = cacheInfo.getTsLengthMap();
                if (tsLengthMap != null) {
                    long tsLength = tsLengthMap.get(tsIndex) != null ? tsLengthMap.get(tsIndex) : 0;
                    return segFile.length() == tsLength;
                }
            }
        }
        return false;
    }

    public long getTotalSize(String md5) {
        if (TextUtils.isEmpty(md5)) {
            return -1L;
        }
        logger.d("getTotalSize md5: %s, size: %s", md5, mCacheInfoMap.size());
        for(Map.Entry entry : mCacheInfoMap.entrySet()) {
            String url = String.valueOf(entry.getKey());
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            VideoCacheInfo cacheInfo = mCacheInfoMap.get(url);
            if (cacheInfo != null && TextUtils.equals(cacheInfo.getMd5(), md5)) {
                return cacheInfo.getTotalSize();
            }
        }
        return -1L;
    }

    //网络性能数据回调
    private IHttpPipelineListener mHttpPipelineListener = new IHttpPipelineListener() {
        @Override
        public void onRequestStart(String url, String rangeHeader) {

        }

        @Override
        public void onDnsStart(String url, long timeDuration) {

        }

        @Override
        public void onDnsEnd(String url, long timeDuration) {

        }

        @Override
        public void onConnectStart(String url, long timeDuration) {

        }

        @Override
        public void onConnectEnd(String url, long timeDuration) {

        }

        @Override
        public void onConnectFailed(String url, long timeDuration, Exception e) {

        }

        @Override
        public void onConnectAcquired(String url, long timeDuration) {

        }

        @Override
        public void onConnectRelease(String url, long timeDuration) {

        }

        @Override
        public void onRequestHeaderStart(String url, long timeDuration) {

        }

        @Override
        public void onRequestHeaderEnd(String url, long timeDuration) {

        }

        @Override
        public void onRequestBodyStart(String url, long timeDuration) {

        }

        @Override
        public void onRequestBodyEnd(String url, long timeDuration) {

        }

        @Override
        public void onResponseHeaderStart(String url, long timeDuration) {

        }

        @Override
        public void onResponseHeaderEnd(String url, long timeDuration) {

        }

        @Override
        public void onResponseBodyStart(String url, long timeDuration) {

        }

        @Override
        public void onResponseBodyEnd(String url, long timeDuration) {

        }

        @Override
        public void onResponseEnd(String url, long timeDuration) {

        }

        @Override
        public void onFailed(String url, long timeDuration, Exception e) {

        }
    };
}

