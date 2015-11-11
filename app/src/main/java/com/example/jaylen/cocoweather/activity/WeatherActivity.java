package com.example.jaylen.cocoweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.jaylen.cocoweather.R;
import com.example.jaylen.cocoweather.service.AutoUpdateService;
import com.example.jaylen.cocoweather.utils.HttpCallbackListener;
import com.example.jaylen.cocoweather.utils.HttpUtil;
import com.example.jaylen.cocoweather.utils.Utility;

import org.json.JSONObject;

import java.util.HashMap;

public class WeatherActivity extends Activity {
    private LinearLayout weatherLayout;

    /**
     * 由于显示城市名
     */
    private TextView cityNameText;

    /**
     * 用于显示发布时间savedInstanceState
     */
    private TextView publishText;

    /**
     * 用于显示是天气描述信息edInstanceState
     */
    private TextView weatherDespText;

    /**
     * Disp Temp1vedInstanceState
     */
    private TextView temp1Text;

    /**
     * Disp Temp2
     */
    private TextView temp2Text;

    /**
     * Disp Current Date
     */
    private TextView currentDateText;

    /**
     * Switch City
     */
    private Button switchCity;

    /**
     * Updata Weather Info
     */
    private  Button refreshWeather;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);

        //Init Control
        weatherLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
        cityNameText = (TextView)findViewById(R.id.city_name);
        publishText = (TextView)findViewById(R.id.publish_text);
        weatherDespText = (TextView)findViewById(R.id.weather_desp);
        temp1Text = (TextView)findViewById(R.id.temp1);
        temp2Text = (TextView)findViewById(R.id.temp2);
        currentDateText = (TextView)findViewById(R.id.weather_date);
        switchCity = (Button)findViewById(R.id.switch_city);
        refreshWeather = (Button)findViewById(R.id.refresh_weather);
        String countyCode = getIntent().getStringExtra("county_code");
        if(!TextUtils.isEmpty(countyCode)){
            //有县级代号是就去查询天气
            publishText.setText("同步中...");
            weatherLayout.setVisibility(View.INVISIBLE);
            cityNameText.setVisibility(View.INVISIBLE);
            queryWeatherInfo(countyCode);
        }else {
            //没有县级代码时直接显示本地天气
            showWeather();
        }
        switchCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherActivity.this,Choose_Area.class);
                intent.putExtra("from_weather_activity",true);
                startActivity(intent);
                finish();
            }
        });
        refreshWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishText.setText("同步中...");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                String weatherCode = prefs.getString("weather_code","");
                if (!TextUtils.isEmpty(weatherCode)){
                    queryWeatherInfo(weatherCode);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.check_update:
                showProgessDialog();
                checkUpdate();
                break;
            case  R.id.about:
                Intent intent = new Intent(WeatherActivity.this,AboutActivity.class);
                startActivity(intent);
                break;
            default:
        }
        return true;
    }

    /**
     * 检查更新
     */
    private void checkUpdate(){
        String address = "http://api.fir.im/apps/latest/"+R.string.AppID+"?api_token="+R.string.ApiTooken;
        final HashMap hashMap = new HashMap();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                try{
                    JSONObject appInfo = new JSONObject(response);
                    hashMap.put("name",appInfo.getString("name"));
                    hashMap.put("version",appInfo.getString("version"));
                    hashMap.put("changelog",appInfo.getString("changelog"));
                    hashMap.put("versionShort",appInfo.getString("versionShort"));
                    hashMap.put("build",appInfo.getString("build"));
                    hashMap.put("installUrl",appInfo.getString("installUrl"));
                    hashMap.put("install_url",appInfo.getString("install_url"));
                    hashMap.put("update_url",appInfo.getString("update_url"));
                    hashMap.put("fsize", appInfo.getJSONObject("binary").getString("fsize"));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(Exception e) {

            }
        },"fir.im");
        closeProgressDialog();
    }
    /**
     * 查询天气代号所对应的天气。
     */
    private void queryWeatherInfo(String weatherCode) {
        String address = "http://apis.baidu.com/apistore/weatherservice/cityname？cityid=" +
                weatherCode;
        queryFromServer(address, "weatherCode");
    }
    /**
     * 根据传入的地址和类型去向服务器查询天气代号或者天气信息。
     */
    private void queryFromServer(final String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {
                if ("countyCode".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        // 从服务器返回的数据中解析出天气代号
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2) {
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                } else if ("weatherCode".equals(type)) {
                    // 处理服务器返回的天气信息
                    Utility.handleWeatherResponse(WeatherActivity.this,
                            response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publishText.setText("同步失败");
                    }
                });
            }
        }, "weather");
        }
    /**
     * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上。
     */
    private void showWeather() {
        SharedPreferences prefs = PreferenceManager.
                getDefaultSharedPreferences(this);
        cityNameText.setText( prefs.getString("city_name", ""));
        temp1Text.setText(prefs.getString("temp1", ""));
        temp2Text.setText(prefs.getString("temp2", ""));
        weatherDespText.setText(prefs.getString("weather_desp", ""));
        publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
        currentDateText.setText(prefs.getString("current_date", ""));
        weatherLayout.setVisibility(View.VISIBLE);
        cityNameText.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
    /**
     * 显示进度对话框
     */
    private void showProgessDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("正在检查更新，请稍后...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    }
