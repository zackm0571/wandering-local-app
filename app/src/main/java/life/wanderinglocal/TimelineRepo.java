package life.wanderinglocal;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.SearchResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static life.wanderinglocal.Constants.DEFAULT_RESULT_LIMIT;

/**
 * This repo populates the timeline from a variety of different data sources.
 * Todo: Ads, Google Places API, WanderingLocal API, limit by distance, change sorting parameters.
 */
public class TimelineRepo {
    private static final double MIN_RATING = 4.0;
    private YelpApi yelpApi;

    public MutableLiveData<List<WLTimelineEntry>> getData() {
        if (data == null) {
            data = new MutableLiveData<>();
            data.setValue(new ArrayList<>());
        }
        return data;
    }

    private MutableLiveData<List<WLTimelineEntry>> data = new MutableLiveData<>();
    private MutableLiveData<WLCategory> searchingBy = new MutableLiveData<>(); // Store filtration options in WLCategory?

    private Handler handler = new Handler(Looper.getMainLooper());
    private WLDatabase db;
    private Context context;
    private String location, lat, lng;
    private Listener listener;
    private OkHttpClient client = new OkHttpClient();

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private String getLat() {
        if (lat == null) lat = "";
        return lat;
    }

    private String getLng() {
        if (lng == null) lng = "";
        return lng;
    }

    public String getLocation() {
        if (location == null) location = "";
        return location;
    }

    public void setLocation(String location) {
        if (location == null) location = "";
        this.location = location;
    }

    public void setLocation(String lat, String lng) {
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * @return the {@link WLCategory} that's currently being used to search.
     */
    @NonNull
    public MutableLiveData<WLCategory> getSearchingBy() {
        if (searchingBy.getValue() == null) {
            searchingBy.setValue(CategoryRepo.DEFAULT_SEARCH_CATEGORY);
        }
        return searchingBy;
    }

    /**
     * Sets the {@link WLCategory} used for searching / populating the timeline.
     *
     * @param category
     */
    public void setSearchBy(@NonNull WLCategory category) {
        searchingBy.setValue(category);
    }

    public void setSearchBy(String s) {
        searchingBy.setValue(new WLCategory(s));
    }

    //todo: replace with observer
    public interface Listener {
        void onDataLoaded();

        void onDataPersisted();
    }

    public TimelineRepo(Context context) {
        this.context = context;
        yelpApi = new YelpApi();
        if (ServiceLocator.getDb() == null) {
            db = ServiceLocator.buildDb(context);
        } else {
            db = ServiceLocator.getDb();
        }
        loadCached();
    }

    private MutableLiveData<List<WLTimelineEntry>> search(YelpApi.SearchBuilder builder) {
        Timber.d("Search: location=%s, lat=%s, lng=%s, searchTerm=%s", getLocation(), getLat(), getLng(), getSearchingBy().getValue().toString());
        if (getLat().length() == 0 && getLng().length() == 0 && getLocation().length() == 0)
            return data;
        yelpApi.search(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                SearchResponse searchResponse = response.body();
                ArrayList<Business> businesses = searchResponse.getBusinesses();
                List<WLTimelineEntry> results = new ArrayList<>();
                for (Business b : businesses) {
                    WLTimelineEntry data = new WLTimelineEntry();
                    data.setBusinessName(b.getName());
                    data.setImageUrl(b.getImageUrl());
                    data.setYelpUrl(b.getUrl());
                    data.setRating(b.getRating());
                    data.setSearchTerm(getSearchingBy().getValue().getName()); //todo update db to include serialized WLCategory
                    data.setLocation(b.getLocation());
                    data.setDistance(b.getDistance());
                    results.add(data);
                }
                Timber.d("Yelp search has returned %d results", results.size());
                Collections.sort(results, (t1, t2) -> Double.compare(t2.getRating(), t1.getRating()));
                data.postValue(results);
                if (listener != null) listener.onDataLoaded();
                if (results.size() > 0) {
                    persist(results);
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Timber.e(call.request().toString());
                AsyncTask.execute(() -> {
                    if (db != null) {
                        List<WLTimelineEntry> cached = db.dao().getDataWithParams(getSearchingBy().getValue().getName(), MIN_RATING);
                        if (cached.size() > 0) {
                            Collections.sort(cached, (t1, t2) -> Double.compare(t2.getRating(), t1.getRating()));
                            data.postValue(cached);
                        }
                    }
                });
            }
        }, builder);
        return data;
    }

    public MutableLiveData<List<WLTimelineEntry>> search() {
        return search(new YelpApi.SearchBuilder().setLimit(DEFAULT_RESULT_LIMIT).setLatLng(getLat(), getLng()).setLocation(getLocation()).setTerm(getSearchingBy().getValue().getName()));
    }

    public MutableLiveData<List<WLTimelineEntry>> searchWithOffset(int offset) {
        return search(new YelpApi.SearchBuilder().setLimit(DEFAULT_RESULT_LIMIT).setOffset(offset).setLatLng(getLat(), getLng()).setLocation(getLocation()).setTerm(getSearchingBy().getValue().getName()));
    }

    private void persist(List<WLTimelineEntry> entries) {
        if (ServiceLocator.getDb() == null)
            Timber.d("DB is null, not persisting results");
        AsyncTask.execute(() -> {
            ServiceLocator.getDb().dao().addEntries(entries);
            if (listener != null) listener.onDataPersisted();
            Timber.d("Persisting %d items to db", entries.size());
        });
    }

    private void loadCached() {
        Timber.d("loadCached");
        final String searchTerm = getSearchingBy().getValue().getName();
        AsyncTask.execute(() -> {
            List<WLTimelineEntry> cached = db.dao().getDataWithParams(searchTerm, MIN_RATING);
            if (cached != null && cached.size() > 0 && (data.getValue() == null || data.getValue().size() == 0)) {
                handler.post(() -> {
                    Collections.sort(cached, (t1, t2) -> Double.compare(t2.getRating(), t1.getRating()));
                    data.setValue(cached);
                });
            }
        });
    }
}
