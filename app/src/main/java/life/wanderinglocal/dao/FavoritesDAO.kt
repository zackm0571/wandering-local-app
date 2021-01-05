package life.wanderinglocal.dao

import androidx.room.*
import life.wanderinglocal.WLTimelineEntry

@Entity(tableName = "favorites")
interface FavoritesDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEntry(data: WLTimelineEntry)

    @Delete
    suspend fun deleteEntry(data: WLTimelineEntry)

    @Query("SELECT * FROM favorites")
    suspend fun getFavorites() : List<WLTimelineEntry>
}