package life.wanderinglocal.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class WanderingWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WanderingWidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
