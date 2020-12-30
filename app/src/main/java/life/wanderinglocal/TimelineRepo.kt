package life.wanderinglocal

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.yelp.fusion.client.models.SearchResponse
import life.wanderinglocal.Constants.DEFAULT_RESULT_LIMIT
import life.wanderinglocal.YelpApi.SearchBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*

/**
 * This repo populates the timeline from a variety of different data sources.
 * Todo: Ads, Google Places API, WanderingLocal API, limit by distance, change sorting parameters.
 */
class TimelineRepo() {
    private val yelpApi: YelpApi

    private var data: MutableLiveData<List<WLTimelineEntry>?> = MutableLiveData()
    private val searchingBy = MutableLiveData<WLCategory>() // Store filtration options in WLCategory?
    private val handler = Handler(Looper.getMainLooper())
    private var db: WLDatabase? = null
    var location: String? = null
        get() {
            if (field == null) field = ""
            return field
        }
        set(location) {
            var location = location
            if (location == null) location = ""
            field = location
        }
    private var lat: String? = null
    private var lng: String? = null
    private var listener: Listener? = null
    private val client = OkHttpClient()
    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    private fun getLat(): String {
        if (lat == null) lat = ""
        return lat!!
    }

    private fun getLng(): String {
        if (lng == null) lng = ""
        return lng!!
    }

    fun setLocation(lat: String, lng: String) {
        this.lat = lat
        this.lng = lng
    }

    /**
     * @return the [WLCategory] that's currently being used to search.
     */
    fun getSearchingBy(): MutableLiveData<WLCategory> {
        if (searchingBy.value == null) {
            searchingBy.value = CategoryRepo.DEFAULT_SEARCH_CATEGORY
        }
        return searchingBy
    }

    /**
     * Sets the [WLCategory] used for searching / populating the timeline.
     *
     * @param category
     */
    fun setSearchBy(category: WLCategory) {
        searchingBy.value = category
    }

    fun setSearchBy(s: String?) {
        searchingBy.value = WLCategory(s!!)
    }

    //todo: replace with observer
    interface Listener {
        fun onDataLoaded()
        fun onDataPersisted()
    }

    private fun search(builder: SearchBuilder): MutableLiveData<List<WLTimelineEntry>?> {
        Timber.d("Search: location=%s, lat=%s, lng=%s, searchTerm=%s", location, getLat(), getLng(), getSearchingBy().value.toString())
        if ((getLat().isEmpty() || getLng().isEmpty()) && location!!.isEmpty()) return data
        yelpApi.search(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                val searchResponse = response.body()
                searchResponse?.businesses?.let { it ->
                    val results: ArrayList<WLTimelineEntry?> = ArrayList()
                    for (i in 0 until it.size) {
                        val b = it[i]
                        val data = WLTimelineEntry()
                        data.businessName = b.name
                        data.imageUrl = b.imageUrl
                        data.yelpUrl = b.url
                        data.rating = b.rating
                        data.searchTerm = getSearchingBy().value!!.name //todo update db to include serialized WLCategory
                        data.description = b.text ?: ""
                        data.location = b.location
                        data.distance = b.distance
                        results.add(data)
                    }
                    Timber.d("Yelp search has returned ${results.size} results")
                    results.sortBy {
                        it?.rating
                    }
                    data.postValue(results as List<WLTimelineEntry>)
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                Timber.e(call.request().toString())
            }
        }, builder)
        return data
    }

    fun search(): MutableLiveData<List<WLTimelineEntry>?> {
        return search(SearchBuilder().setLimit(DEFAULT_RESULT_LIMIT).setLatLng(getLat(), getLng()).setLocation(location).setTerm(getSearchingBy().value!!.name))
    }

    fun searchWithOffset(offset: Int): MutableLiveData<List<WLTimelineEntry>?> {
        return search(SearchBuilder().setLimit(DEFAULT_RESULT_LIMIT).setOffset(offset).setLatLng(getLat(), getLng()).setLocation(location).setTerm(getSearchingBy().value!!.name))
    }

    private fun persist(entries: List<WLTimelineEntry?>) {
        if (ServiceLocator.getDb() == null) Timber.d("DB is null, not persisting results")
        AsyncTask.execute {
            ServiceLocator.getDb()!!.dao().addEntries(entries)
            if (listener != null) listener!!.onDataPersisted()
            Timber.d("Persisting %d items to db", entries.size)
        }
    }

    private fun loadCached() {
        Timber.d("loadCached")
        val searchTerm = getSearchingBy().value?.name
        AsyncTask.execute {
            val cached: List<WLTimelineEntry>? = db!!.dao().getDataWithParams(searchTerm, MIN_RATING)
            if (cached != null && cached.size > 0 && (data.value == null || data.value!!.isEmpty())) {
                handler.post {
                    cached.sortedBy {
                        it.rating
                    }
                    data.setValue(cached)
                }
            }
        }
    }

    companion object {
        private const val MIN_RATING = 4.0
    }

    init {
        yelpApi = YelpApi()
//        db = if (ServiceLocator.getDb() == null) {
//            ServiceLocator.buildDb(context)
//        } else {
//            ServiceLocator.getDb()
//        }
//        loadCached()
    }
}