package com.zackmathews.myapplication;

import android.content.Context;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private TimelineRepo repo;
    private MutableLiveData<List<YelpData>> data;

    public void initializeRepo(Context context) {
        this.repo = new TimelineRepo(context);
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

    public void setSearchTerm(String searchTerm) {
        repo.setSearchTerm(searchTerm);
    }

    public MutableLiveData<String> getSearchTerm() {
        return repo.getSearchTerm();
    }

    public void refresh() {
        data = repo.search();
    }

    public void getNextPage() {
        data = repo.searchWithOffset((data.getValue() != null) ? data.getValue().size() : 0);
    }
}
