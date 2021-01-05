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

        @Query("SELECT * FROM timelineEntries WHERE search_term = :searchTerm AND rating > :rating LIMIT 10")
        fun getDataWithParams(searchTerm: String?, rating: Double): List<WLTimelineEntry>?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun addEntries(data: List<WLTimelineEntry?>?)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun addEntry(data: WLTimelineEntry)

        @Query("DELETE FROM timelineEntries")
        fun deleteEntries()

        @Query("SELECT * FROM timelineEntries WHERE is_favorite = 1")
        fun getFavorites(): List<WLTimelineEntry>
    }

    @PrimaryKey
    var id: String = ""

    @ColumnInfo(name = "description")
    lateinit var description: String

    @ColumnInfo(name = "business_name")
    var businessName: String = ""

    @ColumnInfo(name = "image_url")
    lateinit var imageUrl: String

    @ColumnInfo(name = "yelp_url")
    lateinit var yelpUrl: String

    @ColumnInfo(name = "rating")
    var rating: Double = 0.0

    @ColumnInfo(name = "search_term")
    lateinit var searchTerm: String

    @ColumnInfo(name = "is_favorite")
    var isFavorite: Boolean = false

    @ColumnInfo(name = "distance")
    var distance = 0.0

    @Ignore
    var location: Location? = null

    @Ignore
    var bmp: Bitmap? = null

    val locationString: String
        get() = if (location == null) "" else String.format("%s\n%s, %s %s", location?.address1, location?.city, location?.state, location?.zipCode)
}
