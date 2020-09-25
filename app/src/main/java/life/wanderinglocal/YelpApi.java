package life.wanderinglocal;

import com.yelp.fusion.client.connection.YelpFusionApi;
import com.yelp.fusion.client.connection.YelpFusionApiFactory;
import com.yelp.fusion.client.models.SearchResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

import static life.wanderinglocal.Constants.DEFAULT_SEARCH_RADIUS;
import static life.wanderinglocal.Constants.SEARCH_BY_OPEN_NOW;
import static life.wanderinglocal.Constants.SORT_RATING;

public class YelpApi {
    private YelpFusionApi api;

    public YelpApi() {
        try {
            YelpFusionApiFactory apiFactory = new YelpFusionApiFactory();
            api = apiFactory.createAPI(BuildConfig.YELP_API_KEY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void search(Callback<SearchResponse> callback, SearchBuilder builder) {
        Call<SearchResponse> call = api.getBusinessSearch(builder.build());
        call.enqueue(callback);
    }

    public static class SearchBuilder {
        public static final String PARAM_TERM = "term";
        public static final String PARAM_LOCATION = "location";
        public static final String PARAM_LIMIT = "limit";
        public static final String PARAM_OFFSET = "offset";
        public static final String PARAM_OPEN_NOW = "open_now";
        public static final String PARAM_SEARCH_RADIUS = "radius";
        public static final String PARAM_LATITUDE = "latitude";
        public static final String PARAM_LONGITUDE = "longitude";
        public static final String PARAM_SORT_BY = "sort_by";
        Map<String, String> map = new HashMap<>();

        public SearchBuilder setTerm(String term) {
            map.put(PARAM_TERM, term);
            return this;
        }

        public SearchBuilder setLocation(String location) {
            if (location.length() > 0) {
                map.put(PARAM_LOCATION, location);
            }
            return this;
        }

        public SearchBuilder setLimit(int limit) {
            map.put(PARAM_LIMIT, String.valueOf(limit));
            return this;
        }

        public SearchBuilder setOffset(int offset) {
            map.put(PARAM_OFFSET, String.valueOf(offset));
            return this;
        }

        public SearchBuilder setLatLng(String lat, String lng) {
            if (lat != null && lng != null && lat.length() > 0 && lng.length() > 0) {
                map.put(PARAM_LATITUDE, lat);
                map.put(PARAM_LONGITUDE, lng);
            }
            return this;
        }

        public Map<String, String> build() {
            // Apply default filters
            map.put(PARAM_SORT_BY, SORT_RATING);
            map.put(PARAM_OPEN_NOW, String.valueOf(SEARCH_BY_OPEN_NOW));
            map.put(PARAM_SEARCH_RADIUS, String.valueOf(DEFAULT_SEARCH_RADIUS));
            return map;
        }
    }
}
