package com.zackmathews.myapplication;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private YelpRepo repo = ServiceLocator.getYelpRepo();
    private MutableLiveData<List<YelpData>> yelpData = repo.search();

    public LiveData<List<YelpData>> getYelpData() {
        if (yelpData == null) yelpData = new MutableLiveData<>();
        return yelpData;
    }

    public void refresh() {
        yelpData = repo.search();
    }

    public void getNextPage() {
        yelpData = repo.searchWithOffset(yelpData.getValue().size());
    }
}
