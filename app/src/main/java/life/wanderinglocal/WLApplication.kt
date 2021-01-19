package life.wanderinglocal

import android.app.Application
import androidx.room.Room
import life.wanderinglocal.repo.FavoritesRepo
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.*

class WLApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        ServiceLocator.wlDb = Room.databaseBuilder(applicationContext, WLDatabase::class.java, applicationContext.packageName).build()
        ServiceLocator.favoritesRepo = FavoritesRepo(HashSet(WLPreferences.loadStringSetPref(applicationContext, Constants.PREF_IS_FAVORITE_KEY)))
    }
}