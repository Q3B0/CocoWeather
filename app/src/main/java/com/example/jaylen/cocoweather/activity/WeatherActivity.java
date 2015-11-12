package com.example.jaylen.cocoweather.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jaylen.cocoweather.R;
import com.example.jaylen.cocoweather.service.AutoUpdateService;
import com.example.jaylen.cocoweather.utils.HttpCallbackListener;
import com.example.jaylen.cocoweather.utils.HttpUtil;
import com.example.jaylen.cocoweather.utils.Utility;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
    private ProgressDialog progressDialog = null;
    /* 更新进度条 */
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    /* 下载中 */
    private static final int DOWNLOAD = 1;
    /* 下载结束 */
    private static final int DOWNLOAD_FINISH = 2;
    /* 保存解析的XML信息 */
    HashMap<String, String> mHashMap;
    /* 下载保存路径 */
    private String mSavePath;
    /* 记录进度条数量 */
    private int progress;
    /* 是否取消更新 */
    private boolean cancelUpdate = false;

    /*下载地址*/
    private String downloadURL = "";

    private HashMap hashMap;
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
        String address = "http://api.fir.im/apps/latest/"+getResources().getString(R.string.AppID)+"?api_token="+getResources().getString(R.string.ApiTooken);
        hashMap = new HashMap();
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
                    int version = Integer.parseInt(hashMap.get("build").toString());
                    checkVersion(version);
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
     * 检查程序版本号
     * @param build 最新版本号
     * @return
     */
    private void checkVersion(int build){
        try{
            //获取当前程序版本号
            PackageManager packageManager = getApplicationContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getApplicationContext().getPackageName(), 0);
            int version = packageInfo.versionCode;
            //判定版本号，确定适当否有版本更新
            if(build > version){
                showNoticeDialog();
            }else {
                Toast.makeText(WeatherActivity.this,"当前已经是最新版本，无需更新",Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
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
    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                // 正在下载
                case DOWNLOAD:
                    // 设置进度条位置
                    mProgress.setProgress(progress);
                    break;
                case DOWNLOAD_FINISH:
                    // 安装文件
                    installApk();
                    break;
                default:
                    break;
            }
        };
    };
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

    /**
     * 显示软件更新对话框
     */
    private void showNoticeDialog()
    {
        // 构造对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(WeatherActivity.this);
        builder.setTitle("版本更新");
        builder.setMessage("发现新版本，是否更新？");
        // 更新
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                // 显示下载对话框
                showDownloadDialog();
            }
        });
        // 稍后更新
        builder.setNegativeButton("稍后更新", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    private void showDownloadDialog() {
        // 构造软件下载对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(WeatherActivity.this);
        builder.setTitle("正在下载...");
        // 给下载对话框增加进度条
        final LayoutInflater inflater = LayoutInflater.from(WeatherActivity.this);
        View v = inflater.inflate(R.layout.softupdate_progress, null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        builder.setView(v);
        // 取消更新
        builder.setNegativeButton("取消更新", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                // 设置取消状态
                cancelUpdate = true;
            }
        });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        // 现在文件
        downloadApk();
    }

    /**
     * 下载APK文件
     */
    private void downloadApk() {
        // 启动新线程下载软件
        new downloadApkThread().start();
    }

    /**
     * 下载文件线程
     */
    private class downloadApkThread extends Thread{
        @Override
        public void run(){
            try{
                //判断SD卡是否存在，并且是否具有读写权限
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    //获得储存卡的路径
                    String sdpath = Environment.getExternalStorageDirectory() + "/";
                    mSavePath = sdpath + "download";
                    URL url = new URL(hashMap.get("install_url").toString());
                    //创建链接
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.connect();
                    //获取文件大小
                    int length = conn.getContentLength();
                    //创建输入流
                    InputStream is = conn.getInputStream();

                    File file = new File(mSavePath);
                    //判断文件目录是否存在
                    if(!file.exists()){
                        file.mkdir();
                    }
                    File apkFile = new File(mSavePath,hashMap.get("name").toString());
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    //缓存
                    byte buf[] = new byte[1024];
                    //写入到文件中
                    do{
                        int numread = is.read(buf);
                        count += numread;
                        //计算出进度条的位置
                        progress = (int)(((float)count / length)*100);
                        //更新进度条
                        mHandler.sendEmptyMessage(DOWNLOAD);
                        if(numread <= 0){
                            //下载完成
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                            break;
                        }
                        //写入文件
                        fos.write(buf,0,numread);
                    }while (!cancelUpdate);//点击取消就停止下载
                    fos.close();
                    is.close();
                }
            }catch (MalformedURLException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            //取消下载对话框显示
            mDownloadDialog.dismiss();
        }
    }

    /**
     * 安装APK
     */
    private void installApk(){
        File apkfile = new File(mSavePath,hashMap.get("name").toString());
        if(!apkfile.exists()){
            return;
        }
        //通过Intent安装APK文件
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + apkfile.toString()),"application/vnd.android.package-archive");
        startActivity(intent);
    }
}
