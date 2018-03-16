package com.ish.awtest2.bean;

/**
 * Created by ish on 2018/3/16.
 */

public class SettingItem {
    private int imageId;

    private String title;

    public SettingItem(String title,int id){
        this.imageId = id;
        this.title = title;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int image) {
        this.imageId = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
