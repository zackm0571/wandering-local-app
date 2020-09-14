package life.wanderinglocal;

import androidx.room.Database;
import androidx.room.RoomDatabase;
@Database(entities = {YelpData.class}, version = 1)
public abstract class WLDatabase extends RoomDatabase {
    public WLDatabase(){}
    public abstract YelpData.YelpDAO dao();

}
