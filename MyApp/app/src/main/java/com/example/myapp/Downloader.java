package com.example.myapp;

import android.content.Context;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import dalvik.system.DexClassLoader;

public class Downloader {
    private static Object DeviceClass;

    public static File downloadDexFile(String urlString, File destinationFile) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentLength = connection.getHeaderField("Content-Length");
                long fileSize = 0;
                if (contentLength != null) {
                    fileSize = Long.parseLong(contentLength);
                }

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                while (((bytesRead = inputStream.read(buffer)) != -1) && totalBytesRead <= fileSize){
                    fileOutputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                fileOutputStream.close();
                inputStream.close();

                Log.d("Downloader", "Dex file downloaded successfully to: " + destinationFile.getAbsolutePath());
                return destinationFile;
            } else {
                Log.e("Downloader", "Error downloading file: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            Log.e("Downloader", "Error downloading file: " + e.getMessage());
            return null;
        }
    }

    public static Object loadClassFromDex(File dexFile, Context context) {
        try {
            DexClassLoader classLoader = new DexClassLoader(
                    dexFile.getAbsolutePath(),
                    null,
                    null,
                    context.getClassLoader()
            );

            Class<?> myClass = classLoader.loadClass("com.example.mylibrary.DeviceInfo");
            return myClass.newInstance();

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Log.e("Downloader", "Error loading class: " + e.getMessage());
        }
        return null;
    }

    public static Object getDex(String urlString, Context context){
        if (DeviceClass != null){
            return DeviceClass;
        }
        File destinationFile = new File(context.getFilesDir(), "downloaded.dex");
        if (destinationFile.exists()) {
            destinationFile.delete();
        }
        File downloadedFile = downloadDexFile(urlString, destinationFile);
        if (downloadedFile != null && downloadedFile.exists()) {
            downloadedFile.setReadOnly();
        }

        assert downloadedFile != null;
        DeviceClass = loadClassFromDex(downloadedFile, context);
        return DeviceClass;
    }
}
