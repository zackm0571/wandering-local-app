package life.wanderinglocal;

import androidx.room.Database;
import androidx.room.RoomDatabase;
@Database(entities = {WLTimelineEntry.class}, version = 2)
public abstract class WLDatabase extends RoomDatabase {
    public WLDatabase(){}
    public abstract WLTimelineEntry.TimelineEntryDAO dao();

}
