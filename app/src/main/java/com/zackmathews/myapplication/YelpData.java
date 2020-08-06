package com.zackmathews.myapplication;

import android.graphics.Bitmap;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Update;

@Entity(tableName = "yelpData")
public class YelpData {
    public YelpData() {
    }
/*
    @Dao
    public interface CacheDAO {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void updateNetworkTimestamp(Long timestamp);
        @Query()
        Long getNetworkTimestamp();
    }

 */
    @Dao
    public interface YelpDAO {
        @Query("SELECT * FROM yelpdata LIMIT 10")
        List<YelpData> getAll();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void addEntries(List<YelpData> data);

        @Update
        void updateEntries(List<YelpData> data);

        @Query("DELETE FROM yelpdata")
        void deleteEntries();
    }

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

    @Ignore
    private Bitmap bmp;

    public String getImageUrl() {
        return imageUrl;
    }

    @ColumnInfo(name = "image_url")
    private String imageUrl;
    @PrimaryKey
    @NonNull
    private String businessName = "";

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @ColumnInfo(name = "yelp_url")
    private String yelpUrl;

    public void setYelpUrl(String url) {
        this.yelpUrl = url;
    }

    public String getYelpUrl() {
        return yelpUrl;
    }

    @ColumnInfo(name = "rating")
    private double rating;

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
