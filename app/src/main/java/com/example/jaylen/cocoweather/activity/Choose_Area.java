package com.example.jaylen.cocoweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.jaylen.cocoweather.R;
import com.example.jaylen.cocoweather.model.City;
import com.example.jaylen.cocoweather.model.CocoWeatherDB;
import com.example.jaylen.cocoweather.model.County;
import com.example.jaylen.cocoweather.model.Province;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chose_area);
    }
}
