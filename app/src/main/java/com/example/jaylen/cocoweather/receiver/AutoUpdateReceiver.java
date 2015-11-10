package com.example.jaylen.cocoweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.jaylen.cocoweather.service.AutoUpdateService;

/**
 * Created by Administrator on 2015/11/9 0009.
 */
public class AutoUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i =new Intent(context, AutoUpdateService.class);
        context.startService(i);
    }
}
