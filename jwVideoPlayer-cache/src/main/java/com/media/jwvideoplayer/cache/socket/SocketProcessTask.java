package com.media.jwvideoplayer.cache.socket;

import android.text.TextUtils;

import com.media.jwvideoplayer.cache.common.VideoCacheException;
import com.media.jwvideoplayer.cache.socket.request.HttpRequest;
import com.media.jwvideoplayer.cache.socket.response.BaseResponse;
import com.media.jwvideoplayer.cache.socket.response.M3U8Response;
import com.media.jwvideoplayer.cache.socket.response.M3U8SegResponse;
import com.media.jwvideoplayer.cache.socket.response.Mp4Response;
import com.media.jwvideoplayer.cache.utils.HttpUtils;
import com.media.jwvideoplayer.cache.utils.ProxyCacheUtils;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketProcessTask implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(SocketProcessTask.class.getName());
    private static AtomicInteger sRequestCountAtomic = new AtomicInteger(0);
    private final Socket mSocket;

    public SocketProcessTask(Socket socket) {
        mSocket = socket;
    }

    @Override
    public void run() {
        sRequestCountAtomic.addAndGet(1);
        logger.i( "sRequestCountAtomic : " + sRequestCountAtomic.get());
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = mSocket.getOutputStream();
            inputStream = mSocket.getInputStream();
            HttpRequest request = new HttpRequest(inputStream, mSocket.getInetAddress());
            while(!mSocket.isClosed()) {
                request.parseRequest();
                BaseResponse response;
                String url = request.getUri();
                url = url.substring(1);
                url = ProxyCacheUtils.decodeUriWithBase64(url);
                logger.d( "request url=" + url);

                long currentTime = System.currentTimeMillis();
                ProxyCacheUtils.setSocketTime(currentTime);
                if (url.contains(ProxyCacheUtils.VIDEO_PROXY_SPLIT_STR)) {
                    String[] videoInfoArr = url.split(ProxyCacheUtils.VIDEO_PROXY_SPLIT_STR);
                    if (videoInfoArr.length < 4) {
                        throw new VideoCacheException("Local Socket Error Argument");
                    }
                    String sourceId = videoInfoArr[0];
                    String videoUrl = videoInfoArr[1];
                    String videoTypeInfo = videoInfoArr[2];
                    String videoHeaders = videoInfoArr[3];

                    Map<String, String> headers = ProxyCacheUtils.str2Map(videoHeaders);

                    if (TextUtils.equals(ProxyCacheUtils.M3U8, videoTypeInfo)) {
                        response = new M3U8Response(request, sourceId, videoUrl, headers, currentTime);
                    } else if (TextUtils.equals(ProxyCacheUtils.NON_M3U8, videoTypeInfo)) {
                        response = new Mp4Response(request, sourceId, videoUrl, headers, currentTime);
                    } else {
                        //无法从已知的信息判定视频信息，需要重新请求
                        HttpURLConnection connection = HttpUtils.getConnection(videoUrl, headers);
                        String contentType = connection.getContentType();
                        if (ProxyCacheUtils.isM3U8Mimetype(contentType)) {
                            response = new M3U8Response(request, sourceId, videoUrl, headers, currentTime);
                        } else {
                            response = new Mp4Response(request, sourceId, videoUrl, headers, currentTime);
                        }
                    }
                    response.sendResponse(mSocket, outputStream);
                } else if (url.contains(ProxyCacheUtils.SEG_PROXY_SPLIT_STR)) {
                    //说明是M3U8 ts格式的文件
                    String[] videoInfoArr = url.split(ProxyCacheUtils.SEG_PROXY_SPLIT_STR);
                    if (videoInfoArr.length < 6) {
                        throw new VideoCacheException("Local Socket for M3U8 ts file Error Argument");
                    }
                    String sourceId = videoInfoArr[0];
                    String parentUrl = videoInfoArr[1];
                    String videoUrl = videoInfoArr[2];
                    String md5 = videoInfoArr[3];
                    String fileName = videoInfoArr[4];
                    String videoHeaders = videoInfoArr[5];
                    Map<String, String> headers = ProxyCacheUtils.str2Map(videoHeaders);
                    response = new M3U8SegResponse(request, sourceId, parentUrl, videoUrl, headers, currentTime, fileName);
                    response.sendResponse(mSocket, outputStream);
                } else {
                    throw new VideoCacheException("Local Socket Error url");
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.w("socket request failed, exception=" + e);
        } finally {
            ProxyCacheUtils.close(outputStream);
            ProxyCacheUtils.close(inputStream);
            ProxyCacheUtils.close(mSocket);
            int count = sRequestCountAtomic.decrementAndGet();
            logger.i( "finally Socket solve count = " + count);
        }
    }
}
