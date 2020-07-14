package com.zackmathews.myapplication;

import android.graphics.Bitmap;

public class YelpData {

    public Bitmap getBmp() {
        return bmp;
    }

    public void setBmp(Bitmap bmp) {
        this.bmp = bmp;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    private Bitmap bmp;

    public String getImageUrl() {
        return imageUrl;
    }

    private String imageUrl;
    private String businessName;

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    private String yelpUrl;
    public void setYelpUrl(String url) {
        this.yelpUrl = url;
    }

    public String getYelpUrl() {
        return yelpUrl;
    }
}
