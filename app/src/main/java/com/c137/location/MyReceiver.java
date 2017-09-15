package com.c137.location;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, MyService.class);
        context.startService(serviceIntent);
    }
}
