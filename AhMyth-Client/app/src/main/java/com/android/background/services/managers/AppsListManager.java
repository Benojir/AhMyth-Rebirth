package com.android.background.services.managers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AppsListManager {

    private static final String TAG = "MADARA";

    public interface OnRetrievedAppListsListener {
        void onRetrievedAppLists(JSONObject appLists);
    }

    public static void getAppLists(Context context, OnRetrievedAppListsListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                JSONObject appLists = new JSONObject();

                try {
                    JSONArray appInfoList = new JSONArray();

                    List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(0);

                    for (int i = 0; i < packs.size(); i++) {

                        PackageInfo packageInfo = packs.get(i);

                        try {

                            String appName = "";

                            if (packageInfo.applicationInfo != null) {
                                appName = packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
                            }

                            String packageName = packageInfo.packageName;
                            String versionName = packageInfo.versionName;

                            JSONObject app = new JSONObject();

                            app.put("appName", appName);
                            app.put("packageName", packageName);
                            app.put("versionName", versionName);

                            appInfoList.put(app);

                        } catch (Exception e) {
                            Log.e(TAG, "run: ", e);
                        }
                    }

                    Comparator<JSONObject> appNameComparator = new Comparator<JSONObject>() {
                        @Override
                        public int compare(JSONObject obj1, JSONObject obj2) {
                            String appName1 = obj1.optString("appName", "");
                            String appName2 = obj2.optString("appName", "");
                            return appName1.compareTo(appName2);
                        }
                    };

                    // Convert the JSONArray to a List of JSONObjects for sorting
                    List<JSONObject> appInfoListAsList = new ArrayList<>();

                    for (int i = 0; i < appInfoList.length(); i++) {
                        appInfoListAsList.add(appInfoList.getJSONObject(i));
                    }

                    // Sort the list using the custom comparator
                    appInfoListAsList.sort(appNameComparator);

                    // Convert the sorted list back to a JSONArray
                    appInfoList = new JSONArray(appInfoListAsList);

                    appLists.put("appsList", appInfoList);

                } catch (JSONException e) {
                    Log.e(TAG, "run: ", e);
                }

                listener.onRetrievedAppLists(appLists);
            }
        }).start();
    }
}
