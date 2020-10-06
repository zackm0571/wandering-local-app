package life.wanderinglocal.widget;

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

import java.util.HashMap;
import java.util.Map;

import life.wanderinglocal.Constants;
import life.wanderinglocal.R;
import life.wanderinglocal.ServiceLocator;
import life.wanderinglocal.TimelineRepo;
import timber.log.Timber;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WanderingWidgetConfigureActivity WanderingWidgetConfigureActivity}
 * todo: set an alarm with an Intent that your AppWidgetProvider receives, using the AlarmManager. Set the alarm type to either ELAPSED_REALTIME or RTC, which will only deliver the alarm when the device is awake. Then set updatePeriodMillis to zero ("0"). (https://developer.android.com/guide/topics/appwidgets)
 */
public class WanderingWidget extends AppWidgetProvider implements TimelineRepo.Listener {
    private Context context;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Timber.d("onUpdate()");
        this.context = context;
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.wandering_widget);
            bindWidget(appWidgetManager, views, appWidgetId);
        }
    }

    private void bindWidget(AppWidgetManager appWidgetManager, RemoteViews views, int appWidgetId) {
        Timber.d("bindWidget()");
        // Init repo
        TimelineRepo repo = WidgetSearchRepo.widgetIdRepoMap.get(appWidgetId);
        if (repo == null) {
            repo = new TimelineRepo(context);
        }
        String lat = getStringPreference(Constants.PREF_LAT_KEY),
                lng = getStringPreference(Constants.PREF_LNG_KEY);
        if (!lat.isEmpty() && !lng.isEmpty()) {
            repo.setLocation(lat, lng);
        } else {
            repo.setLocation(getStringPreference(Constants.PREF_LOCATION_KEY));
        }
        repo.setSearchBy(getStringPreference(Constants.PREF_CATEGORY_KEY + appWidgetId));
        repo.search();
        WidgetSearchRepo.widgetIdRepoMap.put(appWidgetId, repo);
        appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, new ComponentName(context, WanderingWidget.class));
        Timber.d("Binding app widget id: %s", appWidgetId);
        // Bind views
        Intent intent = new Intent(context, WanderingWidgetRemoteViewsService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setRemoteAdapter(R.id.widgetList, intent);
        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.app_name) + " - " + repo.getSearchingBy().getValue().getName());
        // Refresh data when button clicked
        Intent refreshIntent = new Intent(context, WanderingWidget.class);
        refreshIntent.setAction(Constants.WL_ACTION_WIDGET_CLICK);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.refreshButton, PendingIntent.getBroadcast(context.getApplicationContext(), 0, refreshIntent, 0));
        // Open settings activity
        Intent configurationIntent = new Intent(context, WanderingWidgetConfigureActivity.class);
        configurationIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.settingsButton, PendingIntent.getActivity(context, 0, configurationIntent, 0));
        // Update the widget / adapter
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetList);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        this.context = context;
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
        if (action != null
                && (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                || action.equals(Constants.WL_ACTION_WIDGET_CLICK)
                || action.equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE))) {
            if (ServiceLocator.getDb() == null) {
                ServiceLocator.buildDb(context);
            }

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WanderingWidget.class));
            for (int appWidgetId : appWidgetIds) {
                TimelineRepo repo = WidgetSearchRepo.widgetIdRepoMap.get(appWidgetId);
                if (repo == null) {
                    repo = new TimelineRepo(context);
                }
                WidgetSearchRepo.widgetIdRepoMap.put(appWidgetId, repo);
                RemoteViews views = new RemoteViews(
                        context.getPackageName(),
                        R.layout.wandering_widget);
                bindWidget(appWidgetManager, views, appWidgetId);
            }
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

