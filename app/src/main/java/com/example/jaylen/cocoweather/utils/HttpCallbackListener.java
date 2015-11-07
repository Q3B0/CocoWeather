package com.example.jaylen.cocoweather.utils;

/**
 * Created by 马占良 on 2015/11/7.
 */
public interface HttpCallbackListener {
    void onFinish(String response);

    void onError(Exception e);
}


