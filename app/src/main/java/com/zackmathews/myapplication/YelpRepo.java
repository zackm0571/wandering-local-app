package com.zackmathews.myapplication;

import android.os.AsyncTask;

import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.SearchResponse;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YelpRepo {
    private YelpApi yelpApi;
    private MutableLiveData<List<YelpData>> data = new MutableLiveData<>();
    private MvvmDatabase db;
    public YelpRepo() {
        yelpApi = new YelpApi();
        db = ServiceLocator.getDb();
    }

    public MutableLiveData<List<YelpData>> search() {
        yelpApi.search(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                SearchResponse searchResponse = response.body();
                ArrayList<Business> businesses = searchResponse.getBusinesses();
                List<YelpData> results = new ArrayList<>();
                for (Business b : businesses) {
                    YelpData data = new YelpData();
                    data.setBusinessName(b.getName());
                    data.setImageUrl(b.getImageUrl());
                    data.setYelpUrl(b.getUrl());
                    data.setRating(b.getRating());
                    results.add(data);
                }
                data.setValue(results);
                persist(results);
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {

            }
        }, new YelpApi.SearchBuilder().setLimit(20).setLocation("San Francisco").setTerm("Coffee"));

        return data;
    }

    public MutableLiveData<List<YelpData>> searchWithOffset(int offset) {
        final MutableLiveData<List<YelpData>> result = new MutableLiveData<>();

        yelpApi.search(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                SearchResponse searchResponse = response.body();
                int totalNumberOfResult = searchResponse.getTotal();  // 3

                ArrayList<Business> businesses = searchResponse.getBusinesses();
                List<YelpData> results = new ArrayList<>();
                for (Business b : businesses) {
                    YelpData data = new YelpData();
                    data.setBusinessName(b.getName());
                    data.setImageUrl(b.getImageUrl());
                    data.setYelpUrl(b.getUrl());
                    data.setRating(b.getRating());
                    results.add(data);
                }
                data.setValue(results);
                persist(results);
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                AsyncTask.execute(() -> {
                    List<YelpData> cached = db.dao().getAll();
                    data.setValue(cached);
                });
            }
        }, new YelpApi.SearchBuilder().setLimit(20).setOffset(offset).setLocation("San Francisco").setTerm("Coffee"));

        return result;
    }

    private void persist(List<YelpData> data) {
        AsyncTask.execute(() -> ServiceLocator.getDb().dao().addEntries(data));
    }
}
