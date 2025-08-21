package com.android.background.services.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefHelper {

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SharedPrefHelper(Context context) {
        String prefName = "google_service.pref";
        sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUpdatedHost(String host) {
        editor.putString("updatedHost", host);
        editor.apply();
    }

    public String getUpdatedHost() {
        return sharedPreferences.getString("updatedHost", "http://192.168.0.103:9092");
    }
}
