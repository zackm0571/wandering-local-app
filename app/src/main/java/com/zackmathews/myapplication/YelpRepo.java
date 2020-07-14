package com.zackmathews.myapplication;

import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.SearchResponse;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.MutableLiveData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YelpRepo {
    private YelpApi yelpApi;
    private List<YelpData> data = new ArrayList<>();

    public YelpRepo() {
        yelpApi = new YelpApi();
    }

    public MutableLiveData<List<YelpData>> search() {
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
                data.clear();
                data.addAll(results);
                result.postValue(data);
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
            }
        }, new YelpApi.SearchBuilder().setLimit(20).setLocation("San Francisco").setTerm("American"));

        return result;
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
                    results.add(data);
                }
                data.addAll(results);
                result.postValue(data);
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
            }
        }, new YelpApi.SearchBuilder().setLimit(20).setOffset(offset).setLocation("San Francisco").setTerm("American"));

        return result;
    }
}
