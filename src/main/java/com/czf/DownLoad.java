package com.czf;

import com.czf.thread.DownLoadThread;
import com.czf.thread.LogThread;
import com.czf.util.FileUtils;
import com.czf.util.HttpUtils;
import com.czf.util.LogUtils;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownLoad {

    // 下载的线程数量
    public static int DOWNLOAD_THREAD_NUMBER = 5;

    // 下载线程池
    private static ExecutorService executor = Executors.newFixedThreadPool(DOWNLOAD_THREAD_NUMBER + 1);

    //临时文件后缀名
    public static String FILE_TEMP_SUFFIX = ".temp";

    //设置支持的URL协议
    private static HashSet<String> PROTOCAL_SET = new HashSet<>();

    static {
        PROTOCAL_SET.add("http://");
        PROTOCAL_SET.add("https://");
    }

    public static void main(String[] args) throws Exception{
        if(args == null || args.length == 0 || args[0].trim().length() == 0){
            LogUtils.info("没有传入下载链接!!!");
            LogUtils.info("支持http/https格式的链接!");
            return;
        }

        final String url = args[0];

        long count = PROTOCAL_SET.stream().filter(url::startsWith).count();

        if(count == 0){
            LogUtils.info("不支持的协议格式!");
            return;
        }

        LogUtils.info("将要下载:{}", url);
        new DownLoad().download(url);


    }


    public void download(String url) throws Exception{
        String fileName = HttpUtils.getHttpFileName(url);
        //获取已下载的文件的大小
        long localFileSize = FileUtils.getFileContentLength(fileName);

        //获取要下载的网络文件的具体大小
        long httpFileSize = HttpUtils.getHttpFileContentLength(url);

        //判断是否还要下载
        if(localFileSize >= httpFileSize){
            LogUtils.info("{}已经下载完毕!", fileName);
            return ;
        }

        List<Future<Boolean>> futureList = new ArrayList<>();

        if(localFileSize > 0){
            LogUtils.info("开始断点续传 {}",fileName);
        }
        else{
            LogUtils.info("开始下载文件 {}", fileName);
        }

        LogUtils.info("开始下载时间 {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
        long startTime = System.currentTimeMillis();

        //进行下载任务的切分
        splitDownload(url, futureList);

        //添加日志记录线程
        LogThread logThread = new LogThread(httpFileSize);
        Future<Boolean> future = executor.submit(logThread);
        futureList.add(future);

        //开始下载
        for(Future<Boolean> booleanFuture : futureList){
            booleanFuture.get();
        }

        LogUtils.info("{}文件下载完毕，本次下载耗时：{}", fileName, (System.currentTimeMillis() - startTime) / 1000 + "s");
        LogUtils.info("结束下载时间 {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));

        //文件合并
        boolean merge = merge(fileName);
        if(merge){
            clearTemp(fileName);
        }
        LogUtils.info("本次下载完成!");
        System.exit(0);

    }

    /**
     * 切分下载任务到多个线程
     * @param url
     * @param futureList
     * @throws IOException
     */
    public void splitDownload(String url, List<Future<Boolean>> futureList) throws IOException{
        long httpFileContentLength = HttpUtils.getHttpFileContentLength(url);

        //区间划分
        long size = httpFileContentLength / DOWNLOAD_THREAD_NUMBER;
        long lastSize = httpFileContentLength - (httpFileContentLength / DOWNLOAD_THREAD_NUMBER * (DOWNLOAD_THREAD_NUMBER - 1));

        for(int i = 0; i < DOWNLOAD_THREAD_NUMBER; i++){
            long start = i * size;
            long downloadSize = (i == DOWNLOAD_THREAD_NUMBER - 1) ? lastSize : size;
            Long end = start + downloadSize;

            if(start != 0) start++;

            DownLoadThread downLoadThread = new DownLoadThread(url, start, end, httpFileContentLength, i);
            Future<Boolean> future = executor.submit(downLoadThread);
            futureList.add(future);
        }


    }

    /**
     * 合并临时文件
     * @param filename
     * @return
     * @throws IOException
     */
    public boolean merge(String filename) throws IOException{
        LogUtils.info("开始合并文件 {}", filename);
        byte[] buffer = new byte[1024 * 10];
        int len = -1;

        try(RandomAccessFile randomAccessFile = new RandomAccessFile(filename, "rw")){
            for(int i = 0; i < DOWNLOAD_THREAD_NUMBER; i++){
                try(BufferedInputStream bis = new BufferedInputStream(
                        new FileInputStream(filename + FILE_TEMP_SUFFIX + i)
                )){
                    while((len = bis.read(buffer)) != -1){
                        randomAccessFile.write(buffer, 0 , len);
                    }
                }
            }
            LogUtils.info("文件合并完毕 {}", filename);

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean clearTemp(String filename){
        LogUtils.info("开始清理临时文件");
        boolean flag = true;
        for(int i = 0; i < DOWNLOAD_THREAD_NUMBER; i++){
            File file = new File(filename + FILE_TEMP_SUFFIX + i);
            flag = file.delete();
        }
        LogUtils.info("清理临时文件完成");
        return flag;
    }


}
