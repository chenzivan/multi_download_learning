package com.czf.thread;

import com.czf.DownLoad;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class LogThread implements Callable<Boolean> {

    //注意在jvm中，虚拟机会将long当成两个分离的32位来看待，这是无法保证对long操作的原子性的，使用AtomicLong 来实现对long操作的原子性

    public static AtomicLong LOCAL_FINISH_SIZE = new AtomicLong();
    public static AtomicLong DOWNLOAD_SIZE = new AtomicLong();
    public static AtomicLong DOWNLOAD_FINISH_THREAD = new AtomicLong();
    private long httpFileContentLength;

    public LogThread(long httpFileContentLength){
        this.httpFileContentLength = httpFileContentLength;
    }

    @Override
    public Boolean call() throws Exception {
        int[] downSizeArr = new int[DownLoad.DOWNLOAD_THREAD_NUMBER];
        int i = 0;
        double size = 0;
        double mb = 1024d * 1024d;

        String httpFileSize = String.format("%.2f", httpFileContentLength / mb);

        while(DOWNLOAD_FINISH_THREAD.get() != DownLoad.DOWNLOAD_THREAD_NUMBER){
            double downloadSize = DOWNLOAD_SIZE.get();

            downSizeArr[++i % DownLoad.DOWNLOAD_THREAD_NUMBER] = Double.valueOf(downloadSize - size).intValue();
            size = downloadSize;

            //计算每秒速度
            double allSecDownloadSize = Arrays.stream(downSizeArr).sum();
            int speed = (int) ((allSecDownloadSize / 1024d) / (Math.min(i, DownLoad.DOWNLOAD_THREAD_NUMBER)));

            //剩余时间
            double surplusSize = httpFileContentLength - downloadSize - LOCAL_FINISH_SIZE.get();
            String surplusTime = String.format("%.1f", surplusSize / 1024d / speed);
            if (surplusTime.equals("Infinity")) {
                surplusTime = "-";
            }

            //已下大小
            String currentFileSize = String.format("%.2f", downloadSize / mb + LOCAL_FINISH_SIZE.get() / mb);
            String speedLog = String.format(">>> 已经下载 %smb/%smb, 速度 %skb/s, 剩余时间 %s s", currentFileSize, httpFileSize, speed, surplusTime);
            System.out.println("\r");
            System.out.println(speedLog);
            Thread.sleep(1000);
        }
        System.out.println();
        return true;

    }
}
