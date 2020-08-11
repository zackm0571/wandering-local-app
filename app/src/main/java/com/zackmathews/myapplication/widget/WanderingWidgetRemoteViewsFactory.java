package com.zackmathews.myapplication.widget;

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

import com.zackmathews.myapplication.IOUtils;
import com.zackmathews.myapplication.MvvmDatabase;
import com.zackmathews.myapplication.R;
import com.zackmathews.myapplication.ServiceLocator;
import com.zackmathews.myapplication.YelpData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WanderingWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<YelpData> data = new ArrayList<>();
    private MvvmDatabase db;
    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler();

    public WanderingWidgetRemoteViewsFactory(Context applicationContext, Intent intent) {
        this.context = applicationContext;
        Log.d(getClass().getSimpleName(), "constructor");
    }

    @Override
    public void onCreate() {
        Log.d(getClass().getSimpleName(), "onCreate");
        if (ServiceLocator.getDb() == null) {
            throw new IllegalStateException();
        }
        db = ServiceLocator.getDb();
        handler = new Handler(Looper.getMainLooper());
        loadCached();
    }

    private void loadCached() {
        Log.d(getClass().getSimpleName(), "loadCached");
        AsyncTask.execute(() -> {
            List<YelpData> cached = db.dao().getAll();
            if (cached != null && cached.size() > 0) {
                data = cached;
                Collections.sort(data, (t1, t2) -> Double.compare(t2.getRating(), t1.getRating()));
            }
        });
    }

    @Override
    public void onDataSetChanged() {
        Log.d(getClass().getSimpleName(), "onDataSetChanged");
//        loadCached();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        Log.d(getClass().getSimpleName(), String.format("getCount() size = %d", data.size()));
        return (data != null) ? data.size() : 0;
    }

    @Override
    public RemoteViews getViewAt(int i) {
        Log.d(getClass().getSimpleName(), String.format("getViewAt(%d)", i));
        if (i == AdapterView.INVALID_POSITION || data == null || data.size() == 0) return null;

        YelpData yd = data.get(i);
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.yelp_business_row_widget);
        rv.setTextViewText(R.id.businessName, yd.getBusinessName());
        rv.setTextViewText(R.id.businessRatingText, String.format(Locale.getDefault(),
                "%.2f stars", yd.getRating()));
        AsyncTask.execute(() -> loadImage(yd, rv));
        return rv;
    }

    private void loadImage(YelpData yd, final RemoteViews rv) {
        if (yd.getBmp() != null) {
            rv.setImageViewBitmap(R.id.businessImg, yd.getBmp());
            return;
        }
        Request request = new Request.Builder()
                .url(yd.getImageUrl())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                byte[] img = IOUtils.byteArrFromInputStream(response.body().byteStream());
                Bitmap bmp = BitmapFactory.decodeByteArray(img, 0, img.length);
                if (bmp != null) {
                    yd.setBmp(bmp);
                    handler.post(() -> rv.setImageViewBitmap(R.id.businessImg, yd.getBmp()));
                }
            }
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
