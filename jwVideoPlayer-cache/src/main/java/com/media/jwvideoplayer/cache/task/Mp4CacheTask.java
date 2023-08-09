package com.media.jwvideoplayer.cache.task;

import com.media.jwvideoplayer.cache.StorageManager;
import com.media.jwvideoplayer.cache.listener.IMp4CacheThreadListener;
import com.media.jwvideoplayer.cache.model.VideoCacheInfo;
import com.media.jwvideoplayer.cache.model.VideoRange;
import com.media.jwvideoplayer.cache.utils.StorageUtils;
import com.media.jwvideoplayer.cache.utils.VideoProxyThreadUtils;
import com.media.jwvideoplayer.cache.utils.VideoRangeUtils;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author joyce.wang 2023/4/8
 *
 * mp4视频单线程优化专用类
 */
public class Mp4CacheTask extends VideoCacheTask {
    private static Logger logger = LoggerFactory.getLogger(Mp4CacheTask.class.getName());
    private Mp4VideoCacheThread mVideoCacheThread;
    private final Object mSegMapLock = new Object();
    private TreeMap<Long, Long> mVideoSegMap;            //本地序列化的range结构
    private TreeMap<Long, VideoRange> mVideoRangeMap;    //已经缓存的video range结构
    private VideoRange mRequestRange;                          //当前请求的video range
    private long mCachedSize;                                  //已经缓存的文件大小

    private String mVideoUrl;

    public Mp4CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        super(cacheInfo, headers);
        mTotalSize = cacheInfo.getTotalSize();
        mVideoSegMap = cacheInfo.getVideoSegMap();
        if (mVideoSegMap == null) {
            mVideoSegMap = new TreeMap<>();
        }
        if (mVideoRangeMap == null) {
            mVideoRangeMap = new TreeMap<>();
        }
        mVideoUrl = cacheInfo.getVideoUrl();
        initVideoSegInfo();
    }

    private void initVideoSegInfo() {
        if (mVideoSegMap.size() == 0) {
            //当前没有缓存,需要从头下载
            mRequestRange = new VideoRange(0, mTotalSize);
        } else {
            for (Map.Entry<Long, Long> entry : mVideoSegMap.entrySet()) {
                //因为mVideoSegMap是顺序存储的,所有这样的操作是可以的
                long start = entry.getKey();
                long end = entry.getValue();
                mVideoRangeMap.put(start, new VideoRange(start, end));
                /**
                 * mVideoRangeMap中的key是起始位置, value是存储的VideoRange结构
                 */
            }
        }
    }

    public VideoRange getRequestRange(long position) {
        if (mVideoRangeMap.size() == 0) {
            return new VideoRange(0, mTotalSize);
        } else {
            long start = -1;
            long end = -1;
            for (Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                long startItem = entry.getValue().getStart();
                long endItem = entry.getValue().getEnd();
                if (position < startItem) {
                    if (start == -1) {
                        start = position;
                    }
                    end = startItem;
                    break;
                } else if (position <= endItem){
                    start = endItem;
                } else {
                    //说明position 在当前的videoRange之后
                }
            }

            if (start == -1) {
                start = position;
            }

            if (end == -1) {
                end = mTotalSize;
            }
            return new VideoRange(start, end);
        }
    }

    private IMp4CacheThreadListener mCacheThreadListener = new IMp4CacheThreadListener() {
        @Override
        public void onCacheFailed(VideoRange range, Exception e) {
            notifyOnTaskFailed(e);
        }

        @Override
        public void onCacheProgress(VideoRange range, long cachedSize, float speed, float percent) {
            notifyOnCacheProgress(cachedSize, speed, percent);
        }

        @Override
        public void onCacheRangeCompleted(VideoRange range) {
            notifyOnCacheRangeCompleted(range.getEnd());
        }

        @Override
        public void onCacheCompleted(VideoRange range) {
            logger.d("onCacheCompleted");
        }
    };

    @Override
    public void startCacheTask() {
        //如果文件缓存完(整个文件，而不是单个缓存分片文件),直接通知完成
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
            return;
        }
        notifyOnTaskStart();//通知task开始
        //获取缓存分片的对象（start/end）
        VideoRange requestRange = getRequestRange(0L);
        logger.d( "startCacheTask requestRange: %s", requestRange.toString());
        //启动线程(线程池方式)进行缓存（下载）
        startVideoCacheThread(requestRange);
    }

    @Override
    public synchronized void pauseCacheTask() {
        logger.i("pauseCacheTask");
        if (mVideoCacheThread != null && mVideoCacheThread.isRunning()) {
            mVideoCacheThread.pause();
            mVideoCacheThread = null;

            if (!mCacheInfo.isCompleted() && mRequestRange != null) {
                long tempRangeStart = mRequestRange.getStart();
                long tempRangeEnd = mCachedSize;
                mRequestRange = new VideoRange(tempRangeStart, tempRangeEnd);
                updateVideoRangeInfo();
            }
        }
    }

    @Override
    public void stopCacheTask() {
        logger.i("stopCacheTask");
        if (mVideoCacheThread != null) {
            mVideoCacheThread.pause();
            mVideoCacheThread = null;
        }
        if (!mCacheInfo.isCompleted() && mRequestRange != null) {
            long tempRangeStart = mRequestRange.getStart();
            long tempRangeEnd = mCachedSize;
            mRequestRange = new VideoRange(tempRangeStart, tempRangeEnd);
            updateVideoRangeInfo();
        }
    }

    @Override
    public void seekToCacheTaskFromClient(float percent) {
        //来自客户端的seek操作
    }

    @Override
    public void seekToCacheTaskFromServer(int segIndex) { }

    @Override
    public void seekToCacheTaskFromServer(long startPosition) {
        //来自服务端的seek操作
        boolean shouldSeekToCacheTask;
        if (mVideoCacheThread != null) {
            if (mVideoCacheThread.isRunning()) {
                shouldSeekToCacheTask = shouldSeekToCacheTask(startPosition);
            } else {
                shouldSeekToCacheTask = true;
            }
        } else {
            shouldSeekToCacheTask = true;
        }
        logger.i("seekToCacheTaskFromServer ====> shouldSeekToCacheTask = %s, startPosition = %s", shouldSeekToCacheTask, startPosition);
        if (shouldSeekToCacheTask) {
            pauseCacheTask();
            VideoRange requestRange = getRequestRange(startPosition);
            startVideoCacheThread(requestRange);
        }
    }

    /**
     * true   ====>  表示重新发起请求
     * false  ====>  表示没有必要重新发起请求
     *
     * @param startPosition
     * @return
     */
    private boolean shouldSeekToCacheTask(long startPosition) {

        //当前文件下载完成, 不需要执行range request请求
        if (mCacheInfo.isCompleted()) {
            return false;
        }
        if (mRequestRange != null) {
            boolean result = mRequestRange.getStart() <= startPosition && startPosition < mRequestRange.getEnd();
            if (result) {
                //当前拖动到的位置已经在request range中了, 没有必要重新发起请求了
                if (mCachedSize >= startPosition) {
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public void resumeCacheTask() {
        if (mVideoCacheThread != null && mVideoCacheThread.isRunning()) {
            //当前mp4缓存线程正在运行中, 没有必要重新启动下载了
            return;
        }
        logger.i("resumeCacheTask");
        if (mCachedSize < mTotalSize) {
            VideoRange requestRange = getRequestRange(mCachedSize);
            startVideoCacheThread(requestRange);
        }
    }

    private void startVideoCacheThread(VideoRange requestRange) {
        mRequestRange = requestRange;
        mVideoCacheThread = new Mp4VideoCacheThread(mCacheInfo.getSourceId(), mVideoUrl, mHeaders, requestRange, mTotalSize, mSaveDir.getAbsolutePath(), mCacheThreadListener);
        VideoProxyThreadUtils.submitRunnableTask(mVideoCacheThread);
    }

    private void notifyOnCacheProgress(long cachedSize, float speed, float percent) {
        mCachedSize = cachedSize;
        mCacheInfo.setCachedSize(cachedSize);
        mCacheInfo.setSpeed(speed);
        mCacheInfo.setPercent(percent);
        mListener.onTaskProgress(percent, mCachedSize, mSpeed);
    }

    private void notifyOnCacheRangeCompleted(long startPosition) {
        //这时候已经缓存好了一段分片,可以更新一下video range数据结构了
        updateVideoRangeInfo();
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
        } else {
            if (startPosition == mTotalSize) {
                //说明已经缓存好,但是整视频中间还有一些洞,但是不影响,可以忽略
            } else {
                //开启下一段视频分片的缓存
                VideoRange requestRange = getRequestRange(startPosition);
                //是否开启下一缓存分片的下载。
                // 这里可以再精准的控制下，按需下载
                startVideoCacheThread(requestRange);
            }
        }

    }

    //这个方法比较关键，针对缓存分片信息进行整合，重叠的部分进行合并，重新生成videoRange列表。更新后把其更新到文件中
    private synchronized void updateVideoRangeInfo() {
        if (mVideoRangeMap.size() > 0) {
            long finalStart = -1;
            long finalEnd = -1;

            long requestStart = mRequestRange.getStart();
            long requestEnd = mRequestRange.getEnd();
            for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                long startResult = VideoRangeUtils.determineVideoRangeByPosition(videoRange, requestStart);
                long endResult = VideoRangeUtils.determineVideoRangeByPosition(videoRange, requestEnd);

                if (finalStart == -1) {
                    if (startResult == 1) {
                        //如果requestStart小于遍历的一个片段的start位置，取requestStart
                        finalStart = requestStart;
                    } else if (startResult == 2) {
                        //如果requestStart在遍历的一个片段的start和end中，取该片段的start
                        finalStart = videoRange.getStart();
                    } else {
                        //先别急着赋值,还要看下一个videoRange
                    }
                }
                if (finalEnd == -1) {
                    if (endResult == 1) {
                        finalEnd = requestEnd;
                    } else if (endResult == 2) {
                        finalEnd = videoRange.getEnd();
                    } else {
                        //先别急着赋值,还要看下一个videoRange
                    }
                }

                //该循环的目的是确定finalStart和finalEnd，用于确定VideoRange
                if (finalStart != -1 && finalEnd != -1) {
                    break;
                }
            }
            if (finalStart == -1) {
                finalStart = requestStart;
            }
            if (finalEnd == -1) {
                finalEnd = requestEnd;
            }

            VideoRange finalVideoRange = new VideoRange(finalStart, finalEnd);

            TreeMap<Long, VideoRange> tempVideoRangeMap = new TreeMap<>();
            for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                if (VideoRangeUtils.containsVideoRange(finalVideoRange, videoRange)) {
                    //如果finalVideoRange包含videoRange
                    if (!tempVideoRangeMap.containsKey(finalVideoRange.getStart())) {//防止重复添加
                        tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                    }
                } else if (VideoRangeUtils.compareVideoRange(finalVideoRange, videoRange) == 1) {
                    //如果两个没有交集,且finalVideoRange的end 小于videoRange的start，则map先加入finalVideoRange再加入videoRange
                    if (!tempVideoRangeMap.containsKey(finalVideoRange.getStart())) {//防止重复添加
                        tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                    }
                    tempVideoRangeMap.put(videoRange.getStart(), videoRange);
                } else if (VideoRangeUtils.compareVideoRange(finalVideoRange, videoRange) == 2) {
                    //如果两个没有交集,且finalVideoRange的start 大于videoRange的end，则map先加入videoRange再加入finalVideoRange
                    tempVideoRangeMap.put(videoRange.getStart(), videoRange);
                    if (!tempVideoRangeMap.containsKey(finalVideoRange.getStart())) {//防止重复添加
                        tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                    }
                }
            }
            mVideoRangeMap.clear();
            mVideoRangeMap.putAll(tempVideoRangeMap);
        } else {
            logger.i("updateVideoRangeInfo--->mRequestRange : " + mRequestRange);
            mVideoRangeMap.put(mRequestRange.getStart(), mRequestRange);
        }

        TreeMap<Long, Long> tempSegMap = new TreeMap<>();
        for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
            VideoRange videoRange = entry.getValue();
            logger.i("updateVideoRangeInfo--->Result videoRange : " + videoRange.toString());
            tempSegMap.put(videoRange.getStart(), videoRange.getEnd());
        }
        synchronized (mSegMapLock) {
            mVideoSegMap.clear();
            mVideoSegMap.putAll(tempSegMap);
        }
        mCacheInfo.setVideoSegMap(mVideoSegMap);

        // 当mVideoRangeMap只有一个片段，并且该ranged是完整的这个那个缓存文件（不是某个子片段），则标记为completed
        if (mVideoRangeMap.size() == 1) {
            VideoRange videoRange = mVideoRangeMap.get(0L);
            logger.i("updateVideoRangeInfo---> videoRange : " + videoRange);
            if (videoRange != null && videoRange.equals(new VideoRange(0, mTotalSize))) {
                logger.i("updateVideoRangeInfo--->Set completed");
                mCacheInfo.setIsCompleted(true);
            }
        }

        //子线程中执行,更新缓存信息文件
        saveVideoInfo();
    }

    @Override
    public long getMp4CachedPosition(long position) {
        if (mVideoCacheThread != null && mVideoCacheThread.isPositionContained(position)) {
            return mVideoCacheThread.getRangeEndPosition();
        }
        for (Map.Entry entry : mVideoRangeMap.entrySet()) {
            VideoRange range = (VideoRange)entry.getValue();
            if (range != null && range.contains(position)) {
                return range.getEnd();
            }
        }
        return 0L;
    }

    @Override
    public void notifyOnTaskCompleted() {
        mCachedSize = mTotalSize;
        StorageManager.getInstance().checkCache(mSaveDir.getAbsolutePath());
        mListener.onTaskCompleted(mTotalSize);
    }
}
