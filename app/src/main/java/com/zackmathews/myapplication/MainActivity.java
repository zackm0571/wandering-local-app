package com.zackmathews.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int SCROLL_THRESHOLD = 250;
    private static final int REQUEST_CODE = 71;

    private RecyclerView recyclerView;
    private YelpApi yelpApi;
    private WLTimeLineAdapter yelpAdapter;
    private MainViewModel viewModel;
    private boolean isLoading = false;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViewModel();
        initLocationServices();
        initRecyclerView();
        ServiceLocator.buildDb(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    initLocationServices();
                }
            }
        }
    }

    private void initViewModel(){
        Log.d(getClass().getSimpleName(), "Initializing view model...");
        viewModel = new ViewModelProvider(getViewModelStore(), ViewModelProvider.AndroidViewModelFactory.
                getInstance(getApplication())).get(MainViewModel.class);
        viewModel.setSearchTerm(WLPreferences.loadStringPref(this, Constants.PREF_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM));
        viewModel.getYelpData().observe(this, new Observer<List<YelpData>>() {
            @Override
            public void onChanged(List<YelpData> yelpData) {
                yelpAdapter.setData(yelpData);
                isLoading = false;
            }
        });
    }

    private void initLocationServices() {
        Log.d(getClass().getSimpleName(), "Initializing location services...");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                Log.d(getClass().getSimpleName(), "Requesting location permissions");
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQUEST_CODE);
                return;
            }
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            viewModel.setLocation(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            viewModel.refresh();
        }
    }

    void initRecyclerView() {
        Log.d(getClass().getSimpleName(), "Initializing recycler view...");
        recyclerView = findViewById(R.id.recyclerView);
        yelpAdapter = new WLTimeLineAdapter(this);
        recyclerView.setAdapter(yelpAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
//                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
//                if (!isLoading) {
//                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() >= yelpAdapter.getItemCount() - 5) {
//                        viewModel.getNextPage();
//                        isLoading = true;
//                    }
//                }
            }
        });
    }
}