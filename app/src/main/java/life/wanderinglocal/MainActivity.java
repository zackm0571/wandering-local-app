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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import life.wanderinglocal.databinding.ActivityMainBinding;
import life.wanderinglocal.databinding.SearchLayoutBinding;
import timber.log.Timber;

import static life.wanderinglocal.Constants.PREF_LAT_KEY;
import static life.wanderinglocal.Constants.PREF_LNG_KEY;

public class MainActivity extends ComponentActivity {
    private static final int REQUEST_CODE = 71;
    // Firebase
    private FirebaseAnalytics mFirebaseAnalytics;
    // View binding
    private ActivityMainBinding mainBinding;
    private SearchLayoutBinding searchBinding;
    // Views + ViewModels
    private View searchView;
    private AlertDialog searchDialog;
    private WLTimeLineAdapter yelpAdapter;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFirebase();
        initUI();
        setContentView(mainBinding.getRoot());
        initViewModel();
        initLocationServices();
        initAdmob();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (getSearchDialog().isShowing()) {
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

    private void initFirebase() {
        Timber.d("Initializing Firebase...");
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        FirebaseCrashlytics.getInstance().sendUnsentReports();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, getClass().getSimpleName());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
    }

    private void initViewModel() {
        Timber.d("Initializing view model...");
        viewModel = new ViewModelProvider(getViewModelStore(), ViewModelProvider.AndroidViewModelFactory.
                getInstance(getApplication())).get(MainViewModel.class);
        viewModel.initializeRepo(this);
        viewModel.setSearchingBy(WLPreferences.loadStringPref(this, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM));

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM,
                viewModel.getSearchingBy().getValue().getName());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);

        viewModel.getTimeline().observe(this, yelpData -> {
            Timber.d("Timeline updated");
            yelpAdapter.setData(yelpData);
            mainBinding.progressBar.setVisibility(View.GONE);
            mainBinding.errorText.setVisibility(yelpData.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getSearchingBy().observe(this, s -> {
            setTitle(getString(R.string.app_name) + " - " + s.getName());
        });

        viewModel.getCategories().observe(this, this::initSearchUI);
        // Try to use last lat / lng if possible.
        String lat = WLPreferences.loadStringPref(MainActivity.this, PREF_LAT_KEY, null);
        String lng = WLPreferences.loadStringPref(MainActivity.this, PREF_LNG_KEY, null);

        if (lat != null && lng != null) {
            viewModel.setLocation(lat, lng);
        }
    }

    private void initAdmob() {
        Timber.d("Initializing Admob...");
        MobileAds.initialize(this);
        AdRequest adRequest = new AdRequest.Builder().build();
        AdView adView = findViewById(R.id.adView);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Timber.e(loadAdError.getMessage());
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad failed to load");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad clicked");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad impression");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }
        });
        adView.loadAd(adRequest);
    }

    private void initLocationServices() {
        Timber.d("Initializing location services...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                Timber.d("Requesting location permissions");
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQUEST_CODE);
                return;
            }
        }
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = getLastKnownLocation(locationManager);
        if (location != null) {
            viewModel.setLocation(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            viewModel.refresh();
        }
    }

    private Location getLastKnownLocation(LocationManager locationManager) {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Timber.d("Requesting location permissions");
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQUEST_CODE);
                return null;
            }
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private void initUI() {
        Timber.d("Initializing UI...");

        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());

        Timber.d("Initializing ProgressBar...");
        mainBinding.progressBar.setVisibility(View.VISIBLE);

        Timber.d("Initializing FloatingActionBar...");
        mainBinding.fab.setOnClickListener(view -> {
            getSearchDialog().show();
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Search");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
        });
        Timber.d("Initializing RecyclerView...");
        yelpAdapter = new WLTimeLineAdapter(this);
        mainBinding.recyclerView.setAdapter(yelpAdapter);
        mainBinding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mainBinding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //todo add PageAdapter
            }
        });

        Timber.d("Initializing search view...");
    }

    /**
     * Initializes the search UI and sets the category filtration options.
     * This function is idempotent as it clears the category views before
     * adding new ones provided by a LiveData object in {@link CategoryRepo}.
     *
     * @param categories
     */
    private void initSearchUI(List<WLCategory> categories) {
        ChipGroup cg = getSearchView().findViewById(R.id.categoryChipGroup);
        cg.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                group.setOnCheckedChangeListener(null);
                for (int i = 0; i < cg.getChildCount(); i++) {
                    View v = cg.getChildAt(i);
                    if (v instanceof Chip) {
                        Chip c = (Chip) v;
                        if (c.getId() != checkedId) {
                            c.setChecked(false);
                        }
                    }
                }
                group.setOnCheckedChangeListener(this);
            }
        });

        // Clear chip children before adding categories.
        for (int i = 0; i < cg.getChildCount(); i++) {
            View v = cg.getChildAt(i);
            if (v instanceof Chip) {
                cg.removeViewAt(i);
            }
        }
        String lastSearch = WLPreferences.loadStringPref(this, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM);
        for (WLCategory category : categories) {
            Chip c = new Chip(this);
            c.setText(category.getName());
            c.setCheckable(true);
            if (category.getName().equals(lastSearch)) {
                c.setChecked(true);
            }
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
            searchDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
            searchBinding = SearchLayoutBinding.bind(searchView);
            searchBinding.searchButton.setOnClickListener(view -> {
                String checkedCategory = getCheckedCategory(searchBinding.categoryChipGroup);
                if (checkedCategory == null || checkedCategory.length() == 0) return;
                Timber.d("Searching for %s", checkedCategory);
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, checkedCategory);
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle);

                mainBinding.progressBar.setVisibility(View.VISIBLE);
                WLPreferences.saveStringPref(this, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, checkedCategory);
                viewModel.setSearchingBy(checkedCategory);
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
