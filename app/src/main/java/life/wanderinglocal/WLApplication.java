package life.wanderinglocal;

import android.app.Application;

import androidx.room.Room;

import java.util.HashSet;

import life.wanderinglocal.repo.FavoritesRepo;
import timber.log.Timber;

public class WLApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        ServiceLocator.wlDb = Room.databaseBuilder(getApplicationContext(), WLDatabase.class, getApplicationContext().getPackageName()).build();
        ServiceLocator.favoritesRepo = new FavoritesRepo(new HashSet<>(WLPreferences.loadStringSetPref(getApplicationContext(), Constants.PREF_IS_FAVORITE_KEY)));
    }
}
