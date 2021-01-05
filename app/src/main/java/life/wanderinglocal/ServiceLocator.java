package life.wanderinglocal;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.room.Room;

import life.wanderinglocal.repo.FavoritesRepo;

public class ServiceLocator {
    public static WLDatabase wlDb;
    public static FavoritesRepo favoritesRepo;
}
