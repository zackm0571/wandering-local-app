package life.wanderinglocal;

import android.content.Context;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private TimelineRepo repo;
    private CategoryRepo categoryRepo;
    private MutableLiveData<List<YelpData>> data;
    private MutableLiveData<WLCategory> searchingBy;

    public void initializeRepo(Context context) {
        this.repo = new TimelineRepo(context);
        this.categoryRepo = new CategoryRepo();
        this.data = repo.search();
    }

    public LiveData<List<YelpData>> getYelpData() {
        return data;
    }

    public void setLocation(String location) {
        repo.setLocation(location);
    }

    public void setLocation(String lat, String lng) {
        repo.setLocation(lat, lng);
    }

    /**
     * Sets the search parameters of {@link TimelineRepo}.
     * When //todo add refined search features
     * are implemented, this function may be overridden or {@link WLCategory}
     * may contain a builder. Maybe a SearchQuery and SearchQuery.Builder?
     *
     * @param category {@link WLCategory} containing the search string {@link WLCategory} containing the search string
     */
    public void setSearchingBy(WLCategory category) {
        repo.setSearchBy(category);
    }
    public void setSearchingBy(String s){
        repo.setSearchBy(new WLCategory(s));
    }

    public MutableLiveData<WLCategory> getSearchingBy() {
        searchingBy = repo.getSearchingBy();
        return searchingBy;
    }

    public MutableLiveData<List<WLCategory>> getCategories() {
        return categoryRepo.getCategories();
    }

    public void refresh() {
        data = repo.search();
    }

    public void getNextPage() {
        data = repo.searchWithOffset((data.getValue() != null) ? data.getValue().size() : 0);
    }
}
