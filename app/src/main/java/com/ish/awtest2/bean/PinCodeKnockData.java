package com.ish.awtest2.bean;

import android.util.Log;

import org.litepal.crud.DataSupport;

/**
 * Created by ish on 2018/2/28.
 */

public class PinCodeKnockData extends DataSupport{
    private String userName;
    private String allDataString;

    public void initData(String userName,Double[] array){
        this.userName = userName;
        allDataString = "";
        for (int i = 0; i < array.length-1; i++) {
            allDataString = allDataString + array[i] + ",";
        }
        allDataString = allDataString + array[array.length-1];
    }

    public Double[] getArray(){
        String[] attrs = null;
        attrs = allDataString.split(",");
        Double[] array = new Double[attrs.length];
        for(int i=0;i<attrs.length;i++){
            array[i] = Double.valueOf(attrs[i]);
        }
        return array;
    }

    public String getUserName(){
        return this.userName;
    }
}
