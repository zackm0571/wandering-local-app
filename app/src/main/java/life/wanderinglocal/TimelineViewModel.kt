package life.wanderinglocal

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TimelineViewModel : ViewModel() {
    private val repo: TimelineRepo = TimelineRepo()
    private val categoryRepo: CategoryRepo = CategoryRepo()
    var timeline: MutableLiveData<List<WLTimelineEntry>?> = repo.search()
    val selected: MutableLiveData<WLTimelineEntry?> = MutableLiveData()
    val location: MutableLiveData<Location?> = MutableLiveData()
    val searchingBy: MutableLiveData<WLCategory> = MutableLiveData()

    fun setLocation(lat: Double, lng: Double) {
        repo.setLocation(lat, lng)
        location.value = Location(lat, lng)
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
        searchingBy.value = category
        repo.setSearchBy(category!!)
    }

    fun setSearchingBy(s: String) {
        searchingBy.value = WLCategory(s)
        repo.setSearchBy(WLCategory(s))
    }

    val categories: MutableLiveData<List<WLCategory>>
        get() = categoryRepo.categories

    fun refresh() {
        repo.search()
    }
}