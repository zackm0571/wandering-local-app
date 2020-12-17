package life.wanderinglocal

import android.graphics.Bitmap
import androidx.room.*
import com.yelp.fusion.client.models.Location

/**
 * Model for timeline results. Room uses this as an Entity for database schema.
 * todo: abstract all yelp specific language
 */
@Entity(tableName = "timelineEntries")
class WLTimelineEntry {
    @Dao
    interface TimelineEntryDAO {
        @get:Query("SELECT * FROM timelineEntries LIMIT 20")
        val all: List<WLTimelineEntry?>?

        @Query("SELECT * FROM timelineEntries WHERE searchTerm = :searchTerm AND rating > :rating LIMIT 10")
        fun getDataWithParams(searchTerm: String?, rating: Double): List<WLTimelineEntry>?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun addEntries(data: List<WLTimelineEntry?>?)

        @Update
        fun updateEntries(data: List<WLTimelineEntry?>?)

        @Query("DELETE FROM timelineEntries")
        fun deleteEntries()
    }

    @Ignore
    var bmp: Bitmap? = null

    @PrimaryKey
    var businessName : String = ""

    @ColumnInfo(name = "image_url")
    lateinit var imageUrl: String

    @ColumnInfo(name = "yelp_url")
    lateinit var yelpUrl: String

    @ColumnInfo(name = "rating")
    var rating : Double = 0.0

    @ColumnInfo(name = "searchTerm")
    lateinit var searchTerm: String

    @Ignore
    var location: Location? = null

    val locationString: String
        get() = if (location == null) "" else String.format("%s\n%s, %s %s", location?.address1, location?.city, location?.state, location?.zipCode)

    @ColumnInfo(name = "distance")
    var distance = 0.0

}