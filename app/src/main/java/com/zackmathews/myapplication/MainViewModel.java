package com.zackmathews.myapplication;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private YelpRepo repo = new YelpRepo();
    private MutableLiveData<List<YelpData>> yelpData = repo.search();

    public LiveData<List<YelpData>> getYelpData() {
        return yelpData;
    }

    public void setLocation(String location){
        repo.setLocation(location);
    }

    public void refresh() {
        yelpData = repo.search();
    }

    public void getNextPage() {
        yelpData = repo.searchWithOffset((yelpData.getValue() != null) ? yelpData.getValue().size() : 0);
    }
}
