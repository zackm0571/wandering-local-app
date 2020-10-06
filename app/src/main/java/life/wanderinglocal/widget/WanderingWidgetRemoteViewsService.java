package life.wanderinglocal.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

public class WanderingWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        WanderingWidgetRemoteViewsFactory factory = new WanderingWidgetRemoteViewsFactory(this.getApplicationContext(), intent);
        factory.setAppWidgetId(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0));
        return factory;
    }
}
