package com.zackmathews.myapplication;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private TimelineRepo repo = new TimelineRepo();
    private MutableLiveData<List<YelpData>> yelpData = repo.search();

    public LiveData<List<YelpData>> getYelpData() {
        return yelpData;
    }

    public void setLocation(String location) {
        repo.setLocation(location);
    }
    public void setLocation(String lat, String lng) {
        repo.setLocation(lat, lng);
    }
    public void setSearchTerm(String searchTerm) {
        repo.setSearchTerm(searchTerm);
    }

    public void refresh() {
        yelpData = repo.search();
    }

    public void getNextPage() {
        yelpData = repo.searchWithOffset((yelpData.getValue() != null) ? yelpData.getValue().size() : 0);
    }
}
