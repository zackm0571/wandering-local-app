package life.wanderinglocal;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

/**
 * This repo acts as the provider of filter categories.
 * Categories are held within the {@link WLCategory} model class for extensibility.
 * At some point this class may be converted into a SearchRepo. (Provider?)
 */
public class CategoryRepo {
    public static final WLCategory DEFAULT_SEARCH_CATEGORY = new WLCategory(Constants.DEFAULT_SEARCH_TERM);
    private static final boolean fetchFromNetwork = false;
    private MutableLiveData<List<WLCategory>> categories = new MutableLiveData<>();

    public MutableLiveData<List<WLCategory>> getCategories() {
        if (!fetchFromNetwork) {
            categories.postValue(fetchCategoriesFromDefault());
        }
        return categories;
    }

    /**
     * Converts a static list of strings into a list of {@link WLCategory}.
     *
     * @return a default list of {@link WLCategory} for search + filtering.
     */
    private List<WLCategory> fetchCategoriesFromDefault() {
        List<WLCategory> result = new ArrayList<>();
        for (String s : Constants.DEFAULT_SEARCH_CATEGORIES) {
            result.add(new WLCategory(s));
        }
        return result;
    }
}
