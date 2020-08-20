package com.zackmathews.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.lifecycle.MutableLiveData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YelpRepo {
    private static final double MIN_RATING = 4.0;
    private YelpApi yelpApi;

    public MutableLiveData<List<YelpData>> getData() {
        if (data == null) {
            data = new MutableLiveData<>();
            data.setValue(new ArrayList<>());
        }
        return data;
    }

    private Handler handler = new Handler();
    private MutableLiveData<List<YelpData>> data = new MutableLiveData<>();
    private MvvmDatabase db;
    private Context context;
    private String searchTerm;
    private String location;
    private Listener listener;
    private OkHttpClient client = new OkHttpClient();

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSearchTerm() {
        if (searchTerm == null) searchTerm = "";
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public interface Listener {
        void onDataLoaded();
        void onDataPersisted();
    }

    public YelpRepo() {
        yelpApi = new YelpApi();
        db = ServiceLocator.getDb();
        if (db != null) {
            loadCached();
        }
    }

    public YelpRepo(Context context) {
        this.context = context;
        yelpApi = new YelpApi();
        if (ServiceLocator.getDb() == null) {
            ServiceLocator.buildDb(context);
        }
        db = ServiceLocator.getDb();
        loadCached();
    }

    private MutableLiveData<List<YelpData>> search(YelpApi.SearchBuilder builder) {
        if (getLocation() == null || getLocation().length() == 0) return data;
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
                    data.setSearchTerm(getSearchTerm());
                    results.add(data);
                }
                Collections.sort(results, (t1, t2) -> Double.compare(t2.getRating(), t1.getRating()));
                if(results.size() > 0) {
                    data.postValue(results);
                    if (listener != null) listener.onDataLoaded();
                    persist(results);
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                AsyncTask.execute(() -> {
                    Log.e(getClass().getSimpleName(), call.request().toString());
                    List<YelpData> cached = db.dao().getAll();
                    if(cached.size() > 0) {
                        Collections.sort(cached, (t1, t2) -> Double.compare(t2.getRating(), t1.getRating()));
                        data.postValue(cached);
                    }
                });
            }
        }, builder);
        return data;
    }

    public MutableLiveData<List<YelpData>> search() {
        return search(new YelpApi.SearchBuilder().setLimit(20).setLocation(getLocation()).setTerm(getSearchTerm()));
    }

    public MutableLiveData<List<YelpData>> searchWithOffset(int offset) {
        return search(new YelpApi.SearchBuilder().setLimit(20).setOffset(offset).setLocation(getLocation()).setTerm(getSearchTerm()));
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

    private void loadCached() {
        Log.d(getClass().getSimpleName(), "loadCached");
        AsyncTask.execute(() -> {
            List<YelpData> cached = db.dao().getDataWithParams(getSearchTerm(), MIN_RATING);
            if (cached != null && cached.size() > 0 && (data.getValue() == null || data.getValue().size() == 0)) {
                handler.post(() -> {
                    Collections.sort(cached, (t1, t2) -> Double.compare(t2.getRating(), t1.getRating()));
                    data.setValue(cached);
                    for (YelpData yd : data.getValue()) {
                        loadImage(yd);
                    }
                });
            }
        });
    }

    private void loadImage(final YelpData yd) {
        Log.d(getClass().getSimpleName(), "loadImage()");
        Request request = new Request.Builder()
                .url(yd.getImageUrl())
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                byte[] img = IOUtils.byteArrFromInputStream(response.body().byteStream());
                Bitmap bmp = BitmapFactory.decodeByteArray(img, 0, img.length);
                if (bmp != null) {
                    yd.setBmp(bmp);
                }
            }
        });
    }
}
