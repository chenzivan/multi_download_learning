package com.czf.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpUtils {

    /**
     * 建立连接
     * @param url
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpUrlConnection(String url) throws IOException{
        URL httpUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
        //添加header
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36 Edg/113.0.1774.42");

        return httpURLConnection;
    }

    /**
     * 指定文件传输区间建立连接
     * @param url
     * @param start 文件开始字节
     * @param end 文件结束字节
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpUrlConnection(String url, long start, Long end) throws IOException{
        HttpURLConnection httpURLConnection = getHttpUrlConnection(url);

        LogUtils.debug("此线程下载区间:{}-{}",start,end);
        if(end != null){
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + start + "-" + end);
        }
        else {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + start + "-");
        }

        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        for(String key : headerFields.keySet()){
            LogUtils.debug("此线程的响应头{}:{}", key, headerFields.get(key));
        }

        return httpURLConnection;
    }

    /**
     * 获取文件大小
     * @param url
     * @return
     * @throws IOException
     */
    public static long getHttpFileContentLength(String url) throws IOException{
        HttpURLConnection httpURLConnection = getHttpUrlConnection(url);
        long contentLength = httpURLConnection.getContentLength();
        httpURLConnection.disconnect();
        return contentLength;
    }

    /**
     * 获取网络文件的 ETag
     * @param url
     * @return
     * @throws IOException
     */
    public static String getHttpFileEtag(String url) throws IOException{
        HttpURLConnection httpURLConnection = getHttpUrlConnection(url);
        List<String> eTagList = httpURLConnection.getHeaderFields().get("ETag");
        httpURLConnection.disconnect();
        return eTagList.get(0);
    }

    public static String getHttpFileName(String url){
        int indexOf = url.lastIndexOf("/");
        return url.substring(indexOf + 1);
    }


}
