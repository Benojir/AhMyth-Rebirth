package com.android.background.services.managers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.DataOutputStream;
import java.util.List;

public class DeviceInfoManger {

    private static final String TAG = "MADARA";

    public static boolean isDeviceRooted() {

        Process p;

        try {
            p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");

            // Close the terminal
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                return p.exitValue() != 255;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

//    ----------------------------------------------------------------------------------------------

    @SuppressLint("HardwareIds")
    public static String getPhoneNumber(Context context) {

        String phoneNumber = "Unknown";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {

                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptionInfos != null && !subscriptionInfos.isEmpty()) {

                    for (SubscriptionInfo subscriptionInfo : subscriptionInfos) {

                        int subscriptionId = subscriptionInfo.getSubscriptionId();

                        phoneNumber = subscriptionManager.getPhoneNumber(subscriptionId);

                        Log.d(TAG, "getPhoneNumber: " + "Subscription ID: " + subscriptionId + ", Phone Number: " + phoneNumber);
                    }
                } else {
                    Log.d(TAG, "getPhoneNumber: No active subscriptions found");
                }
            }
        } else {
            TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                phoneNumber = tMgr.getLine1Number();
            }
        }

        return phoneNumber;
    }

//    ----------------------------------------------------------------------------------------------

    public static float getBatteryPercentage(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level != -1 && scale != -1) {
                    return (level / (float) scale) * 100;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getBatteryPercentage: ", e); // Handle any exceptions
        }
        return -1; // Return -1 if unable to determine the battery percentage
    }

//    ----------------------------------------------------------------------------------------------


}
