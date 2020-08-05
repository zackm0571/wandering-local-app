package com.zackmathews.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.Manifest;
import android.app.Service;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int SCROLL_THRESHOLD = 250;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission_group.LOCATION};
    private static final int REQUEST_CODE = 71;

    private RecyclerView recyclerView;
    private YelpApi yelpApi;
    private YelpAdapter yelpAdapter;
    private MainViewModel viewModel;
    private boolean isLoading = false;
    private LocationManager locationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRecyclerView();
        ServiceLocator.buildDb(this);
        viewModel = new ViewModelProvider(getViewModelStore(), ViewModelProvider.AndroidViewModelFactory.
                getInstance(getApplication())).get(MainViewModel.class);
        viewModel.getYelpData().observe(this, new Observer<List<YelpData>>() {
            @Override
            public void onChanged(List<YelpData> yelpData) {
                yelpAdapter.setData(yelpData);
                isLoading = false;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void requestPermissions(){
        List<String> permissionsToRequest = new ArrayList<>();
        for(String permission : PERMISSIONS){
            if(shouldShowRequestPermissionRationale(permission)){
                permissionsToRequest.add(permission);
            }
        }
        if(permissionsToRequest.size() > 0) {
            requestPermissions((String[]) permissionsToRequest.toArray(), REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE){
            for(int i = 0; i < permissions.length; i++){
                if(permissions[i].equals(Manifest.permission_group.LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    initLocationServices();
                }
            }
        }
    }
    private void initLocationServices(){
        //todo
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() >= yelpAdapter.getItemCount() - 5) {
                        viewModel.getNextPage();
                        isLoading = true;
                    }
                }
            }
        });
    }
}