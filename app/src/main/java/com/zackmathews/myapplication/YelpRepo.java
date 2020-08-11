package com.zackmathews.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
    private MutableLiveData<List<YelpData>> data = new MutableLiveData<>();
    private MvvmDatabase db;
    private Context context;
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onDataLoaded();

        void onDataPersisted();
    }

    public YelpRepo() {
        yelpApi = new YelpApi();
        db = ServiceLocator.getDb();
    }

    public YelpRepo(Context context) {
        this.context = context;
        yelpApi = new YelpApi();
        if (ServiceLocator.getDb() == null) {
            ServiceLocator.buildDb(context);
        }
        db = ServiceLocator.getDb();
    }

    private MutableLiveData<List<YelpData>> search(YelpApi.SearchBuilder builder) {
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
                if (listener != null) listener.onDataLoaded();
                persist(results);
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                AsyncTask.execute(() -> {
                    List<YelpData> cached = db.dao().getAll();
                    data.setValue(cached);
                });
            }
        }, builder);
        return data;
    }

    public MutableLiveData<List<YelpData>> search() {
        return search(new YelpApi.SearchBuilder().setLimit(20).setLocation("San Francisco").setTerm("Coffee"));
    }

    public MutableLiveData<List<YelpData>> searchWithOffset(int offset) {
        return search(new YelpApi.SearchBuilder().setLimit(20).setOffset(offset).setLocation("San Francisco").setTerm("Coffee"));
    }

    private void persist(List<YelpData> entries) {
        if (ServiceLocator.getDb() == null)
            Log.d(getClass().getSimpleName(), "DB is null, not persisting results");
        AsyncTask.execute(() -> {
            ServiceLocator.getDb().dao().addEntries(entries);
            if (listener != null) listener.onDataPersisted();
            Log.d(getClass().getSimpleName(), String.format("Persisting %d items to db", entries.size()));
        });
    }
}
