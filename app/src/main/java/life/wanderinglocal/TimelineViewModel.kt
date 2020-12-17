package life.wanderinglocal

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TimelineViewModel : ViewModel() {
    private lateinit var repo: TimelineRepo
    private var categoryRepo: CategoryRepo? = null
    private var data: MutableLiveData<List<WLTimelineEntry>?>? = null
    var selected: MutableLiveData<WLTimelineEntry?> = MutableLiveData()
    lateinit var searchingBy: MutableLiveData<WLCategory>

    fun initializeRepo(context: Context) {
        repo = TimelineRepo(context)
        searchingBy = repo.getSearchingBy()
        if (categoryRepo == null) {
            categoryRepo = CategoryRepo()
        }
        data = repo.search()
    }

    val timeline: LiveData<List<WLTimelineEntry>?>?
        get() = data

    fun setLocation(location: String?) {
        repo?.location = location
    }

    fun setLocation(lat: String?, lng: String?) {
        repo?.setLocation(lat, lng)
    }

    /**
     * Sets the search parameters of [TimelineRepo].
     * When //todo add refined search features
     * are implemented, this function may be overridden or [WLCategory]
     * may contain a builder. Maybe a SearchQuery and SearchQuery.Builder?
     *
     * @param category [WLCategory] containing the search string [WLCategory] containing the search string
     */
    fun setSearchingBy(category: WLCategory?) {
        repo?.setSearchBy(category!!)
    }

    fun setSearchingBy(s: String?) {
        repo?.setSearchBy(WLCategory(s!!))
    }

    val categories: MutableLiveData<List<WLCategory>>
        get() = categoryRepo!!.categories

    fun refresh() {
        data = repo?.search()
    }

    val nextPage: Unit
        get() {
            data = repo?.searchWithOffset(if (data!!.value != null) data!!.value!!.size else 0)
        }
}