package com.example.jaylen.cocoweather.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.example.jaylen.cocoweather.R;
import com.example.jaylen.cocoweather.model.City;
import com.example.jaylen.cocoweather.model.CocoWeatherDB;
import com.example.jaylen.cocoweather.model.County;
import com.example.jaylen.cocoweather.model.Province;
import com.example.jaylen.cocoweather.utils.HttpCallbackListener;
import com.example.jaylen.cocoweather.utils.HttpUtil;
import com.example.jaylen.cocoweather.utils.Utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class Choose_Area extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    TextView titleText;
    ListView listView;
    private ArrayAdapter<String> adapter;
    private CocoWeatherDB cocoWeatherDB;
    private List<String> dataList = new ArrayList<String>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;
    /**
     * 选中的省份
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 当前选中的级别
     */
    private int currentLevel;

    /**
     * 是否从WeatherActivity中跳转过来
     * @param savedInstanceState
     */
    private boolean isFromWeatherActivity;
    /**
     * 储存定位得到的城市信息
     */
    private String[] cityInfo = new String[2];
    LocationClient mLocationClient;
    MyLocationListener mMyLocationListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
        SharedPreferences prefs = PreferenceManager.
                getDefaultSharedPreferences(this);
        //已经选择了城市且不是冲WeatherActivity跳转过来，才会直接跳转到WeatherActivity
        if (prefs.getBoolean("city_selected", false)
                && !isFromWeatherActivity) {
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        AlertDialog dialog = null;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chose_area);
        final AlertDialog.Builder builder = new AlertDialog.Builder(Choose_Area.this);
        builder.setMessage("是否要定位到当前城市？");
        builder.setTitle("是否定位？");
        builder.setCancelable(false);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                initLocation();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mLocationClient != null && mLocationClient.isStarted()) {
                    mLocationClient.unRegisterLocationListener(mMyLocationListener);
                    mLocationClient.stop();
                }
                listView = (ListView) findViewById(R.id.list_view);
                titleText = (TextView) findViewById(R.id.title_text);
                adapter = new ArrayAdapter<String>(Choose_Area.this, android.R.layout.simple_expandable_list_item_1, dataList);
                listView.setAdapter(adapter);
                cocoWeatherDB = CocoWeatherDB.getInstence(Choose_Area.this);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (currentLevel == LEVEL_PROVINCE) {
                            selectedProvince = provinceList.get(position);
                            queryCities();
                        } else if (currentLevel == LEVEL_CITY) {
                            selectedCity = cityList.get(position);
                            queryCounties();
                        } else if (currentLevel == LEVEL_COUNTY) {
                            String countyCode = countyList.get(position).getCountyCode();
                            Intent intent = new Intent(Choose_Area.this,
                                    WeatherActivity.class);
                            intent.putExtra("county_code", countyCode);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
                queryProvince();
            }
        });
        dialog =  builder.show();
    }

    private void initLocation(){
        mLocationClient = new LocationClient(getApplicationContext());
        mMyLocationListener = new MyLocationListener();
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("gcj02");//可选，默认gcj02，设置返回的定位结果坐标系，
        int span=1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        if(!option.isOpenGps()){
            option.setOpenGps(false);
        }
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        mLocationClient.setLocOption(option);
        mLocationClient.registerLocationListener(mMyLocationListener);
        mLocationClient.start();
    }
    private class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            cityInfo[0] = bdLocation.getCity().substring(0,bdLocation.getCity().length()-1);
            cityInfo[1] = bdLocation.getDistrict().substring(0,bdLocation.getDistrict().length()-1);
            final String[] weatherCode = new String[1];
            String httpUrl = "http://apis.baidu.com/apistore/weatherservice/citylist?cityname="+ URLEncoder.encode(cityInfo[1]);
            HttpUtil.sendHttpRequest(httpUrl, new HttpCallbackListener() {
                @Override
                public void onFinish(String response) {
                    try{
                        JSONArray jsonArry = new JSONObject(response).getJSONArray("retData");
                        for(int i = 0;i<jsonArry.length();i++){
                            JSONObject city = jsonArry.getJSONObject(i);
                            String district = URLDecoder.decode(city.getString("name_cn"));
                            String cityName = URLDecoder.decode(city.getString("district_cn"));
                            boolean stu =  district.equals(cityInfo[1])&& cityName.equals(cityInfo[0]);
                            if(stu){
                                weatherCode[0] = city.getString("area_id");
                            }
                            if(weatherCode[0] != null){
                                Intent intent = new Intent(Choose_Area.this,WeatherActivity.class);
                                intent.putExtra("county_code", weatherCode[0]);
                                startActivity(intent);

                                finish();
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {

                }
            },"000");

        }
    }

    /**
     * 获取全国所有的生，有限从数据库查询，如果没有数据库再去服务器上查询
     */
    private void queryProvince() {
        provinceList = cocoWeatherDB.loadProvinces();
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }else{
            queryFromServer(null, "province");
        }
    }

    /**
     * 查询省内所有的城市
     */
    private void queryCities() {
        cityList = cocoWeatherDB.loadCities(selectedProvince.getId());
        if(cityList.size()>0){
            dataList.clear();
            for(City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }

    /**
     * 查询选中市内所有的县城
     */
    private void queryCounties() {
        countyList = cocoWeatherDB.loadCounty(selectedCity.getId());
        if(countyList.size()>0){
            dataList.clear();
            for(County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }else {
            queryFromServer(selectedCity.getCityCode(),"county");
        }
    }

    /**
     * 根据传入的类型和代号从数据库查询省市县信息
     * @param code 代码
     * @param type 类型
     */
    private void queryFromServer(final String code, final String type) {
        String address = "";
        if(!TextUtils.isEmpty(code)){
            switch (type){
                case "province":
                    address = "http://apis.baidu.com/3023/weather/province";
                    break;
                case "city":
                    address = "http://apis.baidu.com/3023/weather/city" + "?id=" + code;
                    break;
                case "county":
                    address = "http://apis.baidu.com/3023/weather/town" + "?id=" + code;
                    break;
            }
        }else {
            address = "http://apis.baidu.com/3023/weather/province";
        }
        showProgessDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvincesResponse(cocoWeatherDB,response);
                }else if("city".equals(type)){
                    result = Utility.handleCitiesResponse(cocoWeatherDB,response,selectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountiesResponse(cocoWeatherDB,response,selectedCity.getId());
                }
                if(result){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvince();
                            }else  if("city".equals(type)){
                                queryCities();
                            }else  if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }else {
                    Toast.makeText(Choose_Area.this,"数据加载失败，将自动定位到当前城市",Toast.LENGTH_SHORT).show();
                    initLocation();
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(Choose_Area.this,"数据加载失败，将自动定位到当前城市",Toast.LENGTH_SHORT).show();
                        initLocation();
                    }
                });
            }
        },"address");
    }

    /**
     * 显示进度对话框
     */
    private void showProgessDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed(){
        if(currentLevel == LEVEL_COUNTY){
            queryCities();
        }else if(currentLevel ==LEVEL_CITY){
            queryProvince();
        }else{
            if(isFromWeatherActivity){
                Intent intent = new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }

}
