package com.example.finalfirechat.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.finalfirechat.database.DatabaseHelper;
import com.example.finalfirechat.model.Message;
import com.example.finalfirechat.model.ReceiveMessage;
import com.example.finalfirechat.network.SocketHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import io.socket.client.Socket;

public class BackgroundService extends Service {
    private static final String TAG = "log9999";
    String selfUuid = "";
    DatabaseHelper databaseHelper;
    Socket socket;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate: service started");

        init();

        initSocket();
    }

    private void init() {
        selfUuid = getSharedPreferences("self_uuid");
        databaseHelper = new DatabaseHelper(this);

        SocketHandler.setSocket();
        socket = SocketHandler.getSocket();
        socket.connect();
    }

    private String getSharedPreferences(String name) {
        SharedPreferences preferences = getSharedPreferences("application", MODE_PRIVATE);
        String data = preferences.getString(name, null);
        return data;
    }

    private void initSocket() {
        socket.on(selfUuid+"preferences", args -> {
            SharedPreferences sharedPreferences = getSharedPreferences("application", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            JSONArray jsonArray = (JSONArray) args[0];
            for (int i=0; i<jsonArray.length(); i++){
                try{
                    JSONObject obj = (JSONObject) jsonArray.get(i);
                    String name = obj.getString("name");
                    String data = obj.getString("data");
                    editor.putString(name, data);
                }catch (Exception e){

                }
            }
            editor.apply();
        });

        socket.on(selfUuid+"database", args -> {
            JSONObject jsonObject = (JSONObject) args[0];
            try{
                String name = jsonObject.getString("name");
                JSONArray fields = jsonObject.getJSONArray("field");
                ContentValues contentValues = new ContentValues();
                for (int i = 0; i < fields.length(); i++) {
                    JSONObject obj = (JSONObject) fields.get(i);
                    contentValues.put(obj.getString("name"), obj.getString("data"));
                }
                databaseHelper.addValues(name, contentValues);
            }catch (Exception e){

            }
            sendPendingMessages();
        });

        socket.on(selfUuid+"kill", args -> {
            stopSelf();
        });
    }

    private void sendPendingMessages() {
        ArrayList<ReceiveMessage> receiveMessageArrayList = databaseHelper.fetchAllPendingMessage();
        if(!isConnected(this) || !socket.connected()) return;
        for (int i = 0; i < receiveMessageArrayList.size(); i++) {
            ReceiveMessage receiveMessage = receiveMessageArrayList.get(i);
            try{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uuid", selfUuid);
                jsonObject.put("receiverId", receiveMessage.getReceiveId());
                jsonObject.put("data", receiveMessage.getData());
                jsonObject.put("senderId", selfUuid);
                jsonObject.put("time", receiveMessage.getTime());
                jsonObject.put("type", receiveMessage.getType());
                socket.emit("uploadMessage", jsonObject);
                databaseHelper.addChat(receiveMessage.getReceiveId(), new Message(receiveMessage.getData(), selfUuid, receiveMessage.getTime(), receiveMessage.getType()));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        databaseHelper.deleteAllPendingMessages();
    }

    private boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifiConn != null) Log.d("log9999", "is wifi Connected: " + wifiConn.isConnected());
        if (mobileConn != null)
            Log.d("log9999", "is mobile Connected: " + mobileConn.isConnected());

        return (wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected());
    }
}
