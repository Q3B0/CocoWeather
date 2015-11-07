package com.example.jaylen.cocoweather.utils;

import android.text.TextUtils;

import com.example.jaylen.cocoweather.model.City;
import com.example.jaylen.cocoweather.model.CocoWeatherDB;
import com.example.jaylen.cocoweather.model.County;
import com.example.jaylen.cocoweather.model.Province;

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
                allProvince = response.split("\\[\\,\\]");
            }else {
                allProvince = new String[]{response};
            }
            if(allProvince != null && allProvince.length>0){
                for(String p : allProvince){
                    String[] array = p.replace("\"","").split(",");
                    Province province = new Province();
                    province.setProvinceName(decodeUnicode(array[0]));
                    province.setProvinceCode(array[2]);

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
    public synchronized static boolean handleCitiesResponse(CocoWeatherDB
                                                                       cocoWeatherDB,String response,int provinceId){
        if(!TextUtils.isEmpty(response)){
            response = response.substring(2,response.length()-2);
            String[] allCities = null;
            if(response.contains("[")){
                allCities = response.split("\\[\\,\\]");
            }else {
                allCities = new String[]{response};
            }
            if(allCities != null && allCities.length>0){
                for(String p : allCities){
                    String[] array = p.replace("\"","").split(",");
                    City city = new City();
                    city.setCityName(decodeUnicode(array[0]));
                    city.setCityCode(array[2]);
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
    public synchronized static boolean handleCountiesResponse(CocoWeatherDB
                                                                    cocoWeatherDB,String response,int cityId){
        if(!TextUtils.isEmpty(response)){
            response = response.substring(2,response.length()-2);
            String[] allCounties = null;
            if(response.contains("[")){
                allCounties = response.split("\\[\\,\\]");
            }else {
                allCounties = new String[]{response};
            }
            if(allCounties != null && allCounties.length>0){
                for(String p : allCounties){
                    String[] array = p.replace("\"","").split(",");
                    County county = new County();
                    county.setCountyName(decodeUnicode(array[0]));
                    county.setCountyCode(array[2]);
                    county.setCityId(cityId);
                    cocoWeatherDB.saveCounty(county);
                }
                return  true;
            }
        }
        return false;
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
