package com.media.jwvideoplayer.lib.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessUtils {
    public static final String PROCESS_FLAG_KEY = "process_flag_key";
    public static final String PROCESS_RESULT_KEY = "process_result_key";
    public static Pattern PID_PATTERN = Pattern.compile("pid=(\\d{1,10})");

    /**
     * 这个方法肯能会经常出现stackoverflow错误
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isPackageRunning(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (processInfo.processName != null && processInfo.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static String execCmdsForResult(String... cmds) {
        ProcessResult result = execCmds(false, 5, cmds);
        return result.outMessaage;
    }

    public static String execCmdsForResult(long timeout, String... cmds) {
        ProcessResult result = execCmds(false, timeout, cmds);
        return result.outMessaage;
    }

    public static String exeCommandsWithFormat(String... cmds) {
        ProcessResult result = execCmds(false, 5, "\n", cmds);
        return result.outMessaage;
    }

    /***
     * @param withSu  是否root身份
     * @param timeout 执行命令超时时间，单位秒
     * @param cmds    执行的命令列表
     * @return
     */
    public static ProcessResult execCmds(boolean withSu, long timeout, String... cmds) {
        return execCmds(withSu, timeout, null, cmds);
    }

    public static ProcessResult execCmds(boolean withSu, long timeout, String separate, String... cmds) {
        ProcessResult result = new ProcessResult();
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final StringBuilder sb = new StringBuilder();
            ProcessBuilder processBuilder = new ProcessBuilder(withSu ? "su" : "sh");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            DataOutputStream cmdStream = new DataOutputStream(process.getOutputStream());
            final InputStream stdout = process.getInputStream();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            if (separate != null) sb.append(separate);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                        try {
                            stdout.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();
            for (String cmd : cmds) {
                String realCmd = cmd;
                if (!cmd.endsWith("\n"))
                    realCmd = cmd + "\n";
                cmdStream.writeBytes(realCmd);
            }
            cmdStream.writeBytes("exit\n");
            cmdStream.flush();
            cmdStream.close();
            int execResult = process.waitFor();
            latch.await(timeout, TimeUnit.SECONDS);
            result.isSuccess = (execResult == 0);
            result.outMessaage = sb.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            result.isSuccess = false;
            result.outMessaage = "";
        }
        return result;
    }

    public static String getPid(Process process) {
        if (process == null) return "";
        String processStr = process.toString();
        Matcher matcher = PID_PATTERN.matcher(processStr);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    public static class ProcessResult {
        private boolean isSuccess;
        private String outMessaage;

        public boolean isSuccess() {
            return isSuccess;
        }

        public String outMessaage() {
            return outMessaage;
        }
    }
}
