package life.wanderinglocal.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import life.wanderinglocal.Constants;
import life.wanderinglocal.IOUtils;
import life.wanderinglocal.R;
import life.wanderinglocal.ServiceLocator;
import life.wanderinglocal.TimelineRepo;
import life.wanderinglocal.WLCategory;
import life.wanderinglocal.WLDatabase;
import life.wanderinglocal.YelpData;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static life.wanderinglocal.WLPreferences.loadStringPref;

/**
 * Adapter for widget ListView. Loads data from room db.
 */
public class WanderingWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<YelpData> data = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler();
    private boolean isLoading = false;
    private TimelineRepo repo;
    private MutableLiveData<List<YelpData>> liveData;
    private WLDatabase db;

    public WanderingWidgetRemoteViewsFactory(Context applicationContext, Intent intent) {
        Log.d(getClass().getSimpleName(), "constructor");
        this.context = applicationContext;
        repo = new TimelineRepo(context);
        liveData = repo.getData();
        if (ServiceLocator.getDb() == null) {
            ServiceLocator.buildDb(context);
        }
        db = ServiceLocator.getDb();
    }

    @Override
    public void onCreate() {
        Log.d(getClass().getSimpleName(), "onCreate");
        handler = new Handler(Looper.getMainLooper());
        AsyncTask.execute(() -> data = db.dao().getDataWithParams(repo.getSearchingBy().getValue().getName(), Constants.DEFAULT_MIN_RATING));
    }

    @Override
    public void onDataSetChanged() {
        Log.d(getClass().getSimpleName(), "onDataSetChanged");
        refreshRepo();
        AsyncTask.execute(() -> data = db.dao().getDataWithParams(repo.getSearchingBy().getValue().getName(), Constants.DEFAULT_MIN_RATING));
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        Log.d(getClass().getSimpleName(), String.format("getCount() size = %d", liveData.getValue() != null ? liveData.getValue().size() : 0));
        isLoading = true;
        //todo
        int iterations = 0;
        final int ITRS_BEFORE_SEARCH = 5;
        while ((liveData.getValue() == null || liveData.getValue().size() == 0) && isLoading) {
            iterations++;
            if (iterations >= ITRS_BEFORE_SEARCH) {
                refreshRepo();
            }
            try {
                Log.d(getClass().getSimpleName(), "getCount() blocking wait while images / data loads");
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (liveData.getValue() != null && liveData.getValue().size() > 0) {
                int count = 0;
                for (YelpData yd : liveData.getValue()) {
                    if (yd.getBmp() != null) {
                        count++;
                    }
                }
                if (count == liveData.getValue().size()) {
                    isLoading = false;
                    WanderingWidget.sendRefreshBroadcast(context);
                }
            }
        }

        return liveData.getValue() != null ? liveData.getValue().size() : 0;
    }

    @Override
    public RemoteViews getViewAt(int i) {
        Log.d(getClass().getSimpleName(), String.format("getViewAt(%d)", i));
        if (i == AdapterView.INVALID_POSITION || liveData.getValue() == null || liveData.getValue().size() == 0 || i >= liveData.getValue().size())
            return null;

        YelpData yd = liveData.getValue().get(i);
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.yelp_business_row_widget);
        rv.setTextViewText(R.id.businessName, yd.getBusinessName());
        rv.setTextViewText(R.id.businessRatingText, String.format(Locale.getDefault(),
                "%.2f stars", yd.getRating()));
        if (yd.getBmp() != null) {
            rv.setImageViewBitmap(R.id.businessImg, yd.getBmp());
        } else {
            //            AsyncTask.execute(() -> loadImage(yd));
            final AtomicBoolean isWaiting = new AtomicBoolean();
            isWaiting.set(true);
            Request request = new Request.Builder()
                    .url(yd.getImageUrl())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    isWaiting.set(false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    byte[] img = IOUtils.byteArrFromInputStream(response.body().byteStream());
                    Bitmap bmp = BitmapFactory.decodeByteArray(img, 0, img.length);
                    if (bmp != null) {
                        yd.setBmp(bmp);
                        isWaiting.set(false);
                    }
                }
            });
            //todo
            while (isWaiting.get()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                rv.setImageViewBitmap(R.id.businessImg, yd.getBmp());
            }
        }
        return rv;
    }

    private void refreshRepo() {
        String lat = loadStringPref(context, Constants.PREF_LAT_KEY);
        String lng = loadStringPref(context, Constants.PREF_LNG_KEY);
        String searchTerm = loadStringPref(context, Constants.PREF_CATEGORY_KEY);
        handler.post(() -> {
            repo.setLocation(lat, lng);
            repo.setSearchBy(new WLCategory(searchTerm));
            repo.search();
            Log.d(getClass().getSimpleName(), String.format("Refreshing repo, lat = %s, lng = %s, searchTerm = %s", lat, lng, searchTerm));
        });
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
