package life.wanderinglocal;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends ComponentActivity {
    private static final int SCROLL_THRESHOLD = 250;
    private static final int REQUEST_CODE = 71;

    private View searchView;
    private ProgressBar progressBar;
    private AlertDialog searchDialog;
    private WLTimeLineAdapter yelpAdapter;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViewModel();
        initLocationServices();
        initUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(getSearchDialog().isShowing()){
            getSearchDialog().dismiss();
        }
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

    private void initViewModel() {
        Log.d(getClass().getSimpleName(), "Initializing view model...");

        viewModel = new ViewModelProvider(getViewModelStore(), ViewModelProvider.AndroidViewModelFactory.
                getInstance(getApplication())).get(MainViewModel.class);
        viewModel.initializeRepo(this);
        viewModel.setSearchTerm(WLPreferences.loadStringPref(this, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM));
        viewModel.getYelpData().observe(this, new Observer<List<YelpData>>() {
            @Override
            public void onChanged(List<YelpData> yelpData) {
                yelpAdapter.setData(yelpData);
                progressBar.setVisibility(View.GONE);
            }
        });
        viewModel.getSearchTerm().observe(this, s -> {
            setTitle(getString(R.string.app_name) + " - " + s);
        });
    }

    private void initLocationServices() {
        Log.d(getClass().getSimpleName(), "Initializing location services...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                Log.d(getClass().getSimpleName(), "Requesting location permissions");
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQUEST_CODE);
                return;
            }
        }
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            viewModel.setLocation(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            viewModel.refresh();
        }
    }

    private void initUI() {
        Log.d(getClass().getSimpleName(), "Initializing UI...");
        Log.d(getClass().getSimpleName(), "Initializing ProgressBar...");
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        Log.d(getClass().getSimpleName(), "Initializing FloatingActionBar...");
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            getSearchDialog().show();
        });
        Log.d(getClass().getSimpleName(), "Initializing RecyclerView...");
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        yelpAdapter = new WLTimeLineAdapter(this);
        recyclerView.setAdapter(yelpAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //todo add PageAdapter
            }
        });

        Log.d(getClass().getSimpleName(), "Initializing search view...");
        ChipGroup cg = getSearchView().findViewById(R.id.categoryChipGroup);

        for (String s : Constants.DEFAULT_SEARCH_CATEGORIES) {
            Chip c = new Chip(this);
            c.setText(s);
            c.setCheckable(true);
            cg.addView(c);
        }
    }

    /**
     * Inflates / returns the {@link AlertDialog} containing the search view.
     *
     * @return {@link AlertDialog}
     */
    private AlertDialog getSearchDialog() {
        if (searchDialog == null) {
            searchDialog = new AlertDialog.Builder(this).setView(getSearchView()).create();
            searchDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        return searchDialog;
    }

    /**
     * Returns the search view and inflates it if it has not yet been created.
     *
     * @return View containing the search options.
     */
    private View getSearchView() {
        if (searchView == null) {
            searchView = LayoutInflater.from(this).inflate(R.layout.search_layout, null);
            searchView.findViewById(R.id.searchButton).setOnClickListener(view -> {
                String checkedCategory = getCheckedCategory(searchView.findViewById(R.id.categoryChipGroup));
                if(checkedCategory == null || checkedCategory.length() == 0) return;
                progressBar.setVisibility(View.VISIBLE);
                Log.d(getClass().getSimpleName(), String.format("Searching for %s", checkedCategory));

                WLPreferences.saveStringPref(this, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, checkedCategory);
                viewModel.setSearchTerm(checkedCategory);
                viewModel.refresh();
                getSearchDialog().dismiss();
            });
        }
        return searchView;
    }

    @Nullable
    private String getCheckedCategory(ChipGroup cg) {
        if (cg == null) return null;
        for (int i = 0; i < cg.getChildCount(); i++) {
            View v = cg.getChildAt(i);
            if (v instanceof Chip) {
                Chip c = (Chip) v;
                if (c.isChecked()) {
                    return c.getText().toString();
                }
            }
        }
        return null;
    }
}