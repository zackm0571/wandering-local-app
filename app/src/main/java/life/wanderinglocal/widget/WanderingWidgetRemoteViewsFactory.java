package life.wanderinglocal.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.lifecycle.MutableLiveData;

import life.wanderinglocal.Constants;
import life.wanderinglocal.IOUtils;
import life.wanderinglocal.R;
import life.wanderinglocal.ServiceLocator;
import life.wanderinglocal.WLDatabase;
import life.wanderinglocal.WLTimelineEntry;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static life.wanderinglocal.WLPreferences.loadStringPref;

/**
 * Adapter for widget ListView. Loads data from room db.
 */
public class WanderingWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler();
    private boolean isLoading = false;
    private MutableLiveData<List<WLTimelineEntry>> liveData;
    private WLDatabase db;
    private int appWidgetId = -1;

    public WanderingWidgetRemoteViewsFactory(Context applicationContext, Intent intent) {
        Timber.d("constructor");
        this.context = applicationContext;
        liveData = new MutableLiveData<>();
        if (ServiceLocator.getDb() == null) {
            ServiceLocator.buildDb(context);
        }
        db = ServiceLocator.getDb();
    }

    @Override
    public void onCreate() {
        Timber.d("onCreate");
        handler = new Handler(Looper.getMainLooper());
        refreshRepo();
    }

    @Override
    public void onDataSetChanged() {
        Timber.d("onDataSetChanged");
        refreshRepo();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        Timber.d("getCount() size = %d", liveData.getValue() != null ? liveData.getValue().size() : 0);
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
                Timber.d("getCount() blocking wait while images / data loads");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (liveData.getValue() != null && liveData.getValue().size() > 0) {
                int count = 0;
                for (WLTimelineEntry entry : liveData.getValue()) {
                    if (entry.getBmp() != null) {
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
        Timber.d("getViewAt(%d)", i);
        if (i == AdapterView.INVALID_POSITION || liveData.getValue() == null || liveData.getValue().size() == 0 || i >= liveData.getValue().size())
            return null;

        WLTimelineEntry entry = liveData.getValue().get(i);
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.yelp_business_row_widget);
        rv.setTextViewText(R.id.businessName, entry.getBusinessName());
        rv.setTextViewText(R.id.businessRatingText, String.format(Locale.getDefault(),
                "%.2f stars", entry.getRating()));
        if (entry.getBmp() != null) {
            rv.setImageViewBitmap(R.id.businessImg, entry.getBmp());
        } else {
            final AtomicBoolean isWaiting = new AtomicBoolean();
            isWaiting.set(true);
            Request request = new Request.Builder()
                    .url(entry.getImageUrl())
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
                        entry.setBmp(bmp);
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
                rv.setImageViewBitmap(R.id.businessImg, entry.getBmp());
            }
        }
        return rv;
    }

    private void refreshRepo() {
        if (appWidgetId == -1) {
            Timber.e("AppwidgetId not set");
            return;
        }
        String searchTerm = loadStringPref(context, Constants.PREF_CATEGORY_KEY + appWidgetId, Constants.DEFAULT_SEARCH_TERM);

        Timber.d("Data persisted, loading from category: %s", searchTerm);
        AsyncTask.execute(() -> liveData.postValue(db.dao().getDataWithParams(searchTerm, Constants.DEFAULT_MIN_RATING)));
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

    public int getAppWidgetId() {
        return appWidgetId;
    }

    public void setAppWidgetId(int appWidgetId) {
        this.appWidgetId = appWidgetId;
    }
}
