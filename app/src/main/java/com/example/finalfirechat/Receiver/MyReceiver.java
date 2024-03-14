package com.example.finalfirechat.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.finalfirechat.MainActivity;
import com.example.finalfirechat.services.BackgroundService;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("log9999", "onReceive: called");
        context.startService(new Intent(context, BackgroundService.class));

    }
}
