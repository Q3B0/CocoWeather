package com.example.jaylen.cocoweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.example.jaylen.cocoweather.utils.HttpCallbackListener;
import com.example.jaylen.cocoweather.utils.HttpUtil;
import com.example.jaylen.cocoweather.utils.Utility;

/**
 * Created by Administrator on 2015/11/9 0009.
 */
public class AutoUpdateService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent,int flag,int startId){
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeather();
            }
        }).start();
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int anHour = 2 * 60 * 60 * 1000;//2小时的毫秒数
        long triggerAtTme = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getBroadcast(this,0,i,0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTme,pi);
        return super.onStartCommand(intent,flag,startId);
    }

    /**
     * 更新天气信息
     */
    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherCode = prefs.getString("weather_code", "");
        String address = "http://apis.baidu.com/heweather/weather/free?cityid=CN" +
                weatherCode;
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
           @Override
           public void onFinish(String response) {
               Utility.handleWeatherResponse(AutoUpdateService.this,response);
           }
           @Override
           public void onError(Exception e) {
                e.printStackTrace();
           }
       },"weather");
    }
}
