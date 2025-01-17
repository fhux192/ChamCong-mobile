// FileUtil.java

package com.example.timekeeping;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public java.io.File createTempFile(Context context, InputStream inputStream, String fileName) throws IOException {
        File tempFile = new File(context.getCacheDir(), fileName);
        FileOutputStream out = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.close();
        return tempFile;
    }
}
