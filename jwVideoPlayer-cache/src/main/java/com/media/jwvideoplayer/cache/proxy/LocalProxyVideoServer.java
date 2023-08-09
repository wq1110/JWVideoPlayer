package com.media.jwvideoplayer.cache.proxy;

import com.media.jwvideoplayer.cache.okhttp.OkHttpUtils;
import com.media.jwvideoplayer.cache.socket.SocketProcessTask;
import com.media.jwvideoplayer.cache.utils.ProxyCacheUtils;
import com.media.jwvideoplayer.lib.log.Logger;
import com.media.jwvideoplayer.lib.log.LoggerFactory;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Joyce.wang on 2023/4/14.
 * 本地代理服务端类
 */
public class LocalProxyVideoServer {
    private static Logger logger = LoggerFactory.getLogger(LocalProxyVideoServer.class.getName());
    private final ExecutorService mSocketPool = Executors.newFixedThreadPool(8);

    private ServerSocket mLocalServer;
    private Thread mRequestThread;
    private int mPort;

    public LocalProxyVideoServer() {
        try {
            InetAddress address = InetAddress.getByName(ProxyCacheUtils.LOCAL_PROXY_HOST);
            mLocalServer = new ServerSocket(0, 8, address);
            mPort = mLocalServer.getLocalPort();
            ProxyCacheUtils.getConfig().setPort(mPort);
            ProxyCacheUtils.setLocalPort(mPort);
            CountDownLatch startSignal = new CountDownLatch(1);
            WaitSocketRequestsTask task = new WaitSocketRequestsTask(startSignal);
            mRequestThread = new Thread(task);
            mRequestThread.setName("LocalProxyServerThread");
            mRequestThread.start();
            startSignal.await();
        } catch (Exception e) {
            shutdown();
            logger.w("Cannot create serverSocket, exception=" + e);
        }
    }

    private class WaitSocketRequestsTask implements Runnable {

        private CountDownLatch mLatch;

        public WaitSocketRequestsTask(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void run() {
            mLatch.countDown();
            initSocketProcessor();
        }
    }

    private void initSocketProcessor() {
        do {
            try {
                Socket socket = mLocalServer.accept();
                if (ProxyCacheUtils.getConfig().getConnTimeOut() > 0)
                    socket.setSoTimeout(ProxyCacheUtils.getConfig().getConnTimeOut());
                mSocketPool.submit(new SocketProcessTask(socket));
            } catch (Exception e) {
                logger.w("WaitRequestsRun ServerSocket accept failed, exception=" + e);
            }
        } while (!mLocalServer.isClosed());
    }

    private void shutdown() {
        if (mLocalServer != null) {
            try {
                mLocalServer.close();
            } catch (Exception e) {
                logger.w("ServerSocket close failed, exception=" + e);
            } finally {
                mSocketPool.shutdown();
                if (mRequestThread != null && mRequestThread.isAlive()) {
                    mRequestThread.interrupt();
                }
            }
        }
    }
}