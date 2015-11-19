package com.example.jaylen.cocoweather.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.jaylen.cocoweather.model.City;
import com.example.jaylen.cocoweather.model.CocoWeatherDB;
import com.example.jaylen.cocoweather.model.County;
import com.example.jaylen.cocoweather.model.Province;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by 马占良 on 2015/11/7.
 */
public class Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    public synchronized static boolean handleProvincesResponse(CocoWeatherDB
                                                               cocoWeatherDB,String response){
        if(!TextUtils.isEmpty(response)){
            response = response.substring(2,response.length()-2);
            String[] allProvince = null;
            if(response.contains("[")){
                allProvince = response.split("\\],\\[");
            }else if(!response.contains("\\u")){
                return false;
            } else {
                allProvince = new String[]{response};
            }
            if(allProvince != null && allProvince.length>0){
                for(String p : allProvince){
                    String[] array = p.replace("\"","").split(",");
                    Province province = new Province();
                    province.setProvinceName(decodeUnicode(array[0]));
                    province.setProvinceCode(array[1]);
                    cocoWeatherDB.saveProvince(province);
                }
                return  true;
            }
        }
        return false;
    }

    /**
     * 解析和处理市级数据
     */
    public  static boolean handleCitiesResponse(CocoWeatherDB
                                                                       cocoWeatherDB,String response,int provinceId){
        if(!TextUtils.isEmpty(response)){
            response = response.substring(2,response.length()-2);
            String[] allCities = null;
            if(response.contains("[")){
                allCities = response.split("\\]\\,\\[");
            }else {
                allCities = new String[]{response};
            }
            if(allCities != null && allCities.length>0){
                for(String p : allCities){
                    String[] array = p.replace("\"","").split(",");
                    City city = new City();
                    city.setCityName(decodeUnicode(array[0]));
                    city.setCityCode(array[1]);
                    city.setProvinceId(provinceId);
                    cocoWeatherDB.saveCity(city);
                }
                return  true;
            }
        }
        return false;
    }
    /**
     * 解析和处理所有县级数据
     */
    public  static boolean handleCountiesResponse(CocoWeatherDB

                                                                    cocoWeatherDB,String response,int cityId){
        if(!TextUtils.isEmpty(response)){
            response = response.substring(2,response.length()-2);
            String[] allCounties = null;
            if(response.contains("[")){
                allCounties = response.split("\\]\\,\\[");
            }else {
                allCounties = new String[]{response};
            }
            if(allCounties != null && allCounties.length>0){
                for(String p : allCounties){
                    String[] array = p.replace("\"","").split(",");
                    County county = new County();
                    county.setCountyName(decodeUnicode(array[0]));
                    county.setCountyCode(array[1]);

                    county.setCityId(cityId);
                    cocoWeatherDB.saveCounty(county);
                }
                return  true;
            }
        }
        return false;
    }
    /**
     * 解析服务器返回的JSON数据，并将解析出的数据储存到本地
     */
    public static void handleWeatherResponse(Context context,String response){
        try {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather data service 3.0");
            JSONObject basicInfo = jsonArray.getJSONObject(0).getJSONObject("basic");
            JSONObject day1Info = jsonArray.getJSONObject(0).getJSONArray("daily_forecast").getJSONObject(0);
            JSONObject day2Info = jsonArray.getJSONObject(0).getJSONArray("daily_forecast").getJSONObject(1);
            JSONObject nowInfo= jsonArray.getJSONObject(0).getJSONObject("now");
            JSONObject suggestionInfo = jsonArray.getJSONObject(0).getJSONObject("suggestion");
            saveWeatherInfo(context,basicInfo,nowInfo,day1Info,day2Info,suggestionInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 将服务器返回的信息储存到SharedPreferences文件中
     */
    private static void saveWeatherInfo(Context context, JSONObject basicInfo,JSONObject nowInfo,JSONObject day1Info,JSONObject day2Info,JSONObject suggestionInfo) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        try {
            editor.putBoolean("city_selected", true);
            editor.putString("city_name", basicInfo.getString("city"));
            editor.putString("weather_code", basicInfo.getString("id").substring(2));
            editor.putString("publish_time", basicInfo.getJSONObject("update").getString("loc"));
            editor.putString("weather_desp", nowInfo.getJSONObject("cond").getString("txt"));
            editor.putString("current_temp",nowInfo.getString("tmp"));
            editor.putString("wind_status",nowInfo.getJSONObject("wind").getString("dir")+nowInfo.getJSONObject("wind").getString("sc")+"级");
            editor.putString("weather_img_code",nowInfo.getJSONObject("cond").getString("code")+".png");
            editor.putString("weather_desp_day1", day1Info.getJSONObject("cond").getString("txt_d"));
            editor.putString("weather_img_day1",day1Info.getJSONObject("cond").getString("code_d")+".png");
            editor.putString("weather_desp_day1",day1Info.getJSONObject("cond").getString("txt_n"));
            editor.putString("weather_img_night1",day1Info.getJSONObject("cond").getString("code_n")+".png");
            editor.putString("temp_max1",day1Info.getJSONObject("tmp").getString("max"));
            editor.putString("temp_min1",day1Info.getJSONObject("tmp").getString("min"));
            editor.putString("weather_desp_day2",day2Info.getJSONObject("cond").getString("txt_d"));
            editor.putString("weather_img_day2",day2Info.getJSONObject("cond").getString("code_d")+".png");
            editor.putString("weather_desp_day2",day2Info.getJSONObject("cond").getString("txt_n"));
            editor.putString("weather_img_might2",day2Info.getJSONObject("cond").getString("code_n")+".png");
            editor.putString("temp_max2",day2Info.getJSONObject("tmp").getString("max"));
            editor.putString("temp_min2",day2Info.getJSONObject("tmp").getString("min"));
            editor.putString("comf_brf",suggestionInfo.getJSONObject("comf").getString("brf"));
            editor.putString("comf_txt",suggestionInfo.getJSONObject("comf").getString("txt"));
            editor.putString("cw_brf",suggestionInfo.getJSONObject("cw").getString("brf"));
            editor.putString("cw_txt",suggestionInfo.getJSONObject("cw").getString("txt"));
            editor.putString("drsg_brf",suggestionInfo.getJSONObject("drsg").getString("brf"));
            editor.putString("drsg_txt",suggestionInfo.getJSONObject("drsg").getString("txt"));
            editor.putString("flu_brf",suggestionInfo.getJSONObject("flu").getString("brf"));
            editor.putString("flu_txt",suggestionInfo.getJSONObject("flu").getString("txt"));
            editor.putString("sport_brf",suggestionInfo.getJSONObject("sport").getString("brf"));
            editor.putString("sport_txt",suggestionInfo.getJSONObject("sport").getString("txt"));
            editor.putString("trav_brf",suggestionInfo.getJSONObject("trav").getString("brf"));
            editor.putString("trav_txt",suggestionInfo.getJSONObject("trav").getString("txt"));
            editor.putString("uv_brf",suggestionInfo.getJSONObject("uv").getString("brf"));
            editor.putString("uv_txt",suggestionInfo.getJSONObject("uv").getString("txt"));
        }catch (Exception e){
            e.printStackTrace();
        }
        editor.commit();
    }

    /**
     * 字符编码转换
     * @param theString
     * @return
     */
    private static String decodeUnicode(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len;) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed   \\uxxxx   encoding.");
                        }

                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }
}
