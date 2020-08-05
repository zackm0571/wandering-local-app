package com.zackmathews.myapplication.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.squareup.picasso.Picasso;
import com.zackmathews.myapplication.MvvmDatabase;
import com.zackmathews.myapplication.R;
import com.zackmathews.myapplication.ServiceLocator;
import com.zackmathews.myapplication.YelpData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class WanderingWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private Handler handler = new Handler(Looper.getMainLooper());
    private List<YelpData> data = new ArrayList<>();
    private MvvmDatabase db;

    public WanderingWidgetRemoteViewsFactory(Context applicationContext, Intent intent) {
        this.context = applicationContext;
        if (ServiceLocator.getDb() == null) {
            ServiceLocator.buildDb(applicationContext);
        }
        db = ServiceLocator.getDb();
    }

    @Override
    public void onCreate() {
        loadCached();
    }

    private void loadCached() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<YelpData> cached = db.dao().getAll();
                if (cached != null) {
                    data.clear();
                    data.addAll(cached);
                }
            }
        });
    }

    @Override
    public void onDataSetChanged() {
        WanderingWidget.sendRefreshBroadcast(context);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return (data != null) ? data.size() : 0;
    }

    @Override
    public RemoteViews getViewAt(int i) {
        if (i == AdapterView.INVALID_POSITION) return null;

        YelpData yd = data.get(i);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.yelp_business_row_widget);
        rv.setTextViewText(R.id.businessName, yd.getBusinessName());
        rv.setTextViewText(R.id.businessRatingText, String.format(Locale.getDefault(),
                                                         "%.2f stars", yd.getRating()));
//        try {
//            Bitmap b = Picasso.get().load(yd.getImageUrl()).
//            rv.setImageViewBitmap(R.id.businessImg, b);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        handler.post(() -> Picasso.get().load(yd.getImageUrl()).into(rv, R.id.businessImg, new int[]{i}));

        return rv;
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
