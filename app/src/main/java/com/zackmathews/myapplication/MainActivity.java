package com.zackmathews.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int SCROLL_THRESHOLD = 250;
    private RecyclerView recyclerView;
    private YelpApi yelpApi;
    private YelpAdapter yelpAdapter;
    private MainViewModel viewModel;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRecyclerView();
        viewModel = new ViewModelProvider(getViewModelStore(), ViewModelProvider.AndroidViewModelFactory.
                getInstance(getApplication())).get(MainViewModel.class);
        viewModel.getYelpData().observe(this, new Observer<List<YelpData>>() {
            @Override
            public void onChanged(List<YelpData> yelpData) {
                yelpAdapter.setData(yelpData);
                isLoading = false;
            }
        });
    }

    public void initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        yelpAdapter = new YelpAdapter(this);
        recyclerView.setAdapter(yelpAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == yelpAdapter.getItemCount() - 1) {
                        viewModel.getNextPage();
                        isLoading = true;
                    }
                }
            }
        });
    }
}