package com.media.jwvideoplayer.cache.socket.response;

import android.text.TextUtils;

import com.media.jwvideoplayer.cache.VideoLockManager;
import com.media.jwvideoplayer.cache.VideoProxyCacheManager;
import com.media.jwvideoplayer.cache.common.VideoCacheException;
import com.media.jwvideoplayer.cache.socket.request.HttpRequest;
import com.media.jwvideoplayer.cache.socket.request.ResponseState;
import com.media.jwvideoplayer.cache.utils.ProxyCacheUtils;
import com.media.jwvideoplayer.cache.utils.StorageUtils;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Map;

/**
 * @author joyce.wang
 * MP4视频的local server端
 */
public class Mp4Response extends BaseResponse {
    private static Logger logger = LoggerFactory.getLogger(Mp4Response.class.getName());
    private File mFile;
    private String mMd5;
    private String mSourceId;

    public Mp4Response(HttpRequest request, String sourceId, String videoUrl, Map<String, String> headers, long time) throws Exception {
        super(request, videoUrl, headers, time);
        mSourceId = sourceId;
        mMd5 = ProxyCacheUtils.computeMD5(sourceId);
        mFile = new File(mCachePath, mMd5 + File.separator + mMd5 + StorageUtils.NON_M3U8_SUFFIX);
        mResponseState = ResponseState.OK;
        Object lock = VideoLockManager.getInstance().getLock(mMd5);
        int waitTime = WAIT_TIME;
        mTotalSize = VideoProxyCacheManager.getInstance().getTotalSize(mMd5);
        logger.i("Mp4Response totalSize: %s", mTotalSize);
        //等不到MP4文件大小就不返回
        while (mTotalSize <= 0) {
            synchronized (lock) {
                lock.wait(waitTime);
            }
            mTotalSize = VideoProxyCacheManager.getInstance().getTotalSize(mMd5);
        }

        String rangeStr = request.getRangeString();
        mStartPosition = getRequestStartPosition(rangeStr);
        logger.i( "Range header=%s, start position=%s", request.getRangeString(), mStartPosition);
        if (mStartPosition != -1) {
            mResponseState = ResponseState.PARTIAL_CONTENT;
            //服务端将range起始位置设置到客户端
            VideoProxyCacheManager.getInstance().seekToCacheTaskFromServer(mSourceId, videoUrl, mStartPosition);
        }
    }

    /**
     * 获取range请求的起始位置
     * bytes=15372019-
     * @param rangeStr
     * @return
     */
    private long getRequestStartPosition(String rangeStr) {
        if (TextUtils.isEmpty(rangeStr)) {
            return -1L;
        }
        if (rangeStr.startsWith("bytes=")) {
            rangeStr = rangeStr.substring("bytes=".length());
            if (rangeStr.contains("-")) {
                return Long.parseLong(rangeStr.split("-")[0]);
            }
        }
        return -1L;
    }

    @Override
    public void sendBody(Socket socket, OutputStream outputStream, long pending) throws Exception {
        if (TextUtils.isEmpty(mMd5)) {
            throw new VideoCacheException("Current md5 is illegal, instance="+this);
        }
        Object lock = VideoLockManager.getInstance().getLock(mMd5);
        int waitTime = WAIT_TIME;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(mFile, "r");
            if (randomAccessFile == null) {
                throw new VideoCacheException("Current File is not found, instance="+this);
            }
            int bufferedSize = StorageUtils.DEFAULT_BUFFER_SIZE;
            byte[] buffer = new byte[bufferedSize];
            long offset = mStartPosition == -1L ? 0 : mStartPosition;

            long avilable = VideoProxyCacheManager.getInstance().getMp4CachedPosition(mSourceId, mVideoUrl, offset);

            while(shouldSendResponse(socket, mMd5)) {
                if (avilable == 0) {
                    synchronized (lock) {
                        lock.wait(waitTime = getDelayTime(waitTime));
                    }
                    avilable = VideoProxyCacheManager.getInstance().getMp4CachedPosition(mSourceId, mVideoUrl, offset);
                    waitTime *= 2;
                } else {
                    randomAccessFile.seek(offset);
                    int readLength;

                    long bufferLength = (avilable - offset + 1) > bufferedSize ? bufferedSize : (avilable - offset + 1);

                    while (bufferLength > 0 && (readLength = randomAccessFile.read(buffer, 0, (int) bufferLength)) != -1) {
                        offset += readLength;
                        outputStream.write(buffer, 0, readLength);
                        randomAccessFile.seek(offset);
                        bufferLength = (avilable - offset + 1) > bufferedSize ? bufferedSize : (avilable - offset + 1);
                    }

                    if (offset >= mTotalSize) {
                        logger.i( "Video file is cached in local storage.");
                        break;
                    }
                    if (offset < avilable) {
                        continue;
                    }
                    long lastAvailable = avilable;
                    avilable = VideoProxyCacheManager.getInstance().getMp4CachedPosition(mSourceId, mVideoUrl, offset);
                    waitTime = WAIT_TIME;
                    while(avilable - lastAvailable < bufferedSize && shouldSendResponse(socket, mMd5)) {
                        if (avilable >= mTotalSize - 1) {
                            logger.i( "Video file is cached in local storage.");
                            break;
                        }

                        synchronized (lock) {
                            lock.wait(waitTime = getDelayTime(waitTime));
                        }
                        avilable = VideoProxyCacheManager.getInstance().getMp4CachedPosition(mSourceId, mVideoUrl, offset);
                        waitTime *= 2;
                    }
                }
            }
            logger.i( "Send video info end.");
        } catch (Exception e) {
            logger.e( "Send video info failed.");
            throw e;
        } finally {
            ProxyCacheUtils.close(randomAccessFile);
        }
    }
}
