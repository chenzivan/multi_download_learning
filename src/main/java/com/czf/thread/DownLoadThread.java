package com.czf.thread;

import com.czf.DownLoad;
import com.czf.util.FileUtils;
import com.czf.util.HttpUtils;
import com.czf.util.LogUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

public class DownLoadThread implements Callable<Boolean> {
    /**
     * 每次读取的数据块大小 100KB
     */
    private static int BYTE_SIZE = 1024 * 100;

    /**
     * 下载链接
     */
    private String url;

    /**
     * 开始下载的位置
     */
    private long startPos;

    /**
     * 结束下载的位置
     */
    private Long endPos;

    /**
     * 文件总大小
     */
    private long fileSize;

    /**
     * 标记多线程下载切分的第几部分
     */
    private Integer part;

    public DownLoadThread(String url, long startPos, Long endPos, long fileSize, Integer part){
        this.url = url;
        this.startPos = startPos;
        this.endPos = endPos;
        this.fileSize = fileSize;
        this.part = part;
    }

    @Override
    public Boolean call() throws Exception {
        if (url == null || url.trim().equals("")){
           throw new RuntimeException("下载路径不正确");
        }


        String httpFileName = HttpUtils.getHttpFileName(url);
        if(part != null){
            httpFileName = httpFileName + DownLoad.FILE_TEMP_SUFFIX + part;
        }

        //获取本地文件的大小
        long localFileContentLength = FileUtils.getFileContentLength(httpFileName);
        LogThread.LOCAL_FINISH_SIZE.addAndGet(localFileContentLength);
        if(localFileContentLength >= endPos - startPos){
            //该分片文件已经下载完成
            LogUtils.info("{} 已经下载完毕，无需再次下载!", httpFileName);
            LogThread.DOWNLOAD_FINISH_THREAD.addAndGet(1);
            return true;
        }

        if(endPos.equals(fileSize)){
            endPos = null;
        }

        HttpURLConnection httpURLConnection = HttpUtils.getHttpUrlConnection(url, startPos + localFileContentLength, endPos);

        //获得输入流
        try(InputStream inputStream = httpURLConnection.getInputStream(); BufferedInputStream bis = new BufferedInputStream(inputStream);
            RandomAccessFile randomAccessFile = new RandomAccessFile(httpFileName, "rw")){
            randomAccessFile.seek(localFileContentLength);
            byte[] buffer = new byte[BYTE_SIZE];
            int len = -1;
            //读到文件末尾返回-1
            while((len = bis.read(buffer)) != -1){
                randomAccessFile.write(buffer, 0, len);
                LogThread.DOWNLOAD_SIZE.addAndGet(len);
            }

        }catch (FileNotFoundException e){
            LogUtils.error("要下载的文件路径不存在! {}", url);
            return false;
        }catch (Exception e){
            LogUtils.error("下载发生错误!");
            e.printStackTrace();
            return false;
        }finally {
            httpURLConnection.disconnect();
            LogThread.DOWNLOAD_FINISH_THREAD.addAndGet(1);
        }
        return true;
    }
}
