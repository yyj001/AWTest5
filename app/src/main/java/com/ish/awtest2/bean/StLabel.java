package com.ish.awtest2.bean;

import org.litepal.crud.DataSupport;

/**
 * Created by ish on 2018/4/2.
 */

public class StLabel extends DataSupport{
    private String userName;
    private String allDataString;

    public void initData(String userName,int[] array){
        this.userName = userName;
        allDataString = "";
        for (int i = 0; i < array.length-1; i++) {
            allDataString = allDataString + array[i] + ",";
        }
        allDataString = allDataString + array[array.length-1];
    }

    public String getUserName(){
        return userName;
    }

    public int[] getArray(){
        String[] attrs = null;
        attrs = allDataString.split(",");
        int[] array = new int[attrs.length];
        for(int i=0;i<attrs.length;i++){
            array[i] = Integer.valueOf(attrs[i]);
        }
        return array;
    }
}
