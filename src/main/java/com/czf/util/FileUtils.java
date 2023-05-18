package com.czf.util;

import java.io.File;

public class FileUtils {
    public static long getFileContentLength(String filename){
        File file = new File(filename);

        return file.exists() && file.isFile() ? file.length() : 0;
    }
}
