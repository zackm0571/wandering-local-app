package com.zackmathews.myapplication.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;

import com.zackmathews.myapplication.Constants;
import com.zackmathews.myapplication.R;
import com.zackmathews.myapplication.ServiceLocator;
import com.zackmathews.myapplication.YelpRepo;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WanderingWidgetConfigureActivity WanderingWidgetConfigureActivity}
 * todo: set an alarm with an Intent that your AppWidgetProvider receives, using the AlarmManager. Set the alarm type to either ELAPSED_REALTIME or RTC, which will only deliver the alarm when the device is awake. Then set updatePeriodMillis to zero ("0"). (https://developer.android.com/guide/topics/appwidgets)
 */
public class WanderingWidget extends AppWidgetProvider implements YelpRepo.Listener {
    public static int[] appWidgetIds;
    private YelpRepo repo;
    private Handler handler = new Handler();
    private Context context;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wandering_widget);
        Intent intent = new Intent(context, WanderingWidgetRemoteViewsService.class);
        views.setRemoteAdapter(R.id.widgetList, intent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetList);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WanderingWidget.appWidgetIds = appWidgetIds;
        this.context = context;
        // There may be multiple widgets active, so update all of them
        Log.d(getClass().getSimpleName(), "onUpdate widget");

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.wandering_widget);
            // Set list adapter
            Intent intent = new Intent(context, WanderingWidgetRemoteViewsService.class);
            views.setRemoteAdapter(R.id.widgetList, intent);
            // Refresh data when button clicked
            Intent refreshIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.setComponent(new ComponentName(context, WanderingWidget.class));
            intent.setComponent(new ComponentName(context, WanderingWidgetRemoteViewsFactory.class));
            views.setOnClickPendingIntent(R.id.refreshButton, PendingIntent.getBroadcast(context, 0, refreshIntent, 0));
            // Update the widget / adapter
            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetList);
        }
        if (ServiceLocator.getDb() == null) {
            ServiceLocator.buildDb(context);
        }
        if (repo == null) {
            repo = ServiceLocator.getYelpRepo(context);
        }
        repo.setLocation(getStringPreference(Constants.PREF_LOCATION_KEY));
        repo.setSearchTerm(getStringPreference(Constants.PREF_CATEGORY_KEY));
        repo.setListener(this);
        repo.search();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        this.context = context;
        for (int appWidgetId : appWidgetIds) {
            WanderingWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        this.context = context;
        Log.d(getClass().getSimpleName(), "onEnabled widget");
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        this.context = context;
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        Log.d(getClass().getSimpleName(), "onAppWidgetOptionsChanged");
        this.context = context;
        sendRefreshBroadcast(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        final String action = intent.getAction();
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            if (ServiceLocator.getDb() == null) {
                ServiceLocator.buildDb(context);
            }
            if (repo == null) {
                repo = new YelpRepo(context);
            }
            repo.setListener(this);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, WanderingWidget.class);
            manager.notifyAppWidgetViewDataChanged(manager.getAppWidgetIds(componentName), R.id.widgetList);
        }
        super.onReceive(context, intent);
    }

    public static void sendRefreshBroadcast(Context context) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setComponent(new ComponentName(context, WanderingWidget.class));
        intent.setComponent(new ComponentName(context, WanderingWidgetRemoteViewsFactory.class));
        context.sendBroadcast(intent);
    }

    //todo: access shared prefs from helper
    private String getStringPreference(String key) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, 0);
        return prefs.getString(key, "");
    }

    @Override
    public void onDataLoaded() {
    }

    @Override
    public void onDataPersisted() {
        
    }
}

