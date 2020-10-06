package life.wanderinglocal.widget;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import life.wanderinglocal.CategoryRepo;
import life.wanderinglocal.Constants;
import life.wanderinglocal.LocationUtils;
import life.wanderinglocal.R;
import life.wanderinglocal.WLCategory;
import life.wanderinglocal.WLPreferences;
import timber.log.Timber;

import static life.wanderinglocal.Constants.PREF_LAT_KEY;
import static life.wanderinglocal.Constants.PREF_LNG_KEY;
import static life.wanderinglocal.Constants.PREF_LOCATION_KEY;
import static life.wanderinglocal.WLPreferences.saveStringPref;

/**
 * The configuration screen for the {@link WanderingWidget WanderingWidget} AppWidget.
 */
public class WanderingWidgetConfigureActivity extends ComponentActivity {
    private HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName() + "_" + "thread");
    private CategoryRepo categoryRepo = new CategoryRepo();
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private EditText locationText, customSearchText;
    private CheckBox useMyLocationCheckbox;
    private ProgressBar progressBar;
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            String cityState = LocationUtils.getCityStateFormattedStringFromLocation(WanderingWidgetConfigureActivity.this, location);
            locationText.setText(cityState);
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LOCATION_KEY, cityState);
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LAT_KEY, String.valueOf(location.getLatitude()));
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LNG_KEY, String.valueOf(location.getLongitude()));
            Timber.d("Storing location: %s, lat=%d, lng=%d", cityState, location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };
    CompoundButton.OnCheckedChangeListener useMyLocationClickListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (b) {
                storeLastKnownLocation();
                locationText.setEnabled(false);
            } else {
                locationText.setEnabled(true);
            }
        }
    };

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = WanderingWidgetConfigureActivity.this;

            // Try to use lat / lng if possible, fall back to city, state
            String lat = WLPreferences.loadStringPref(context, PREF_LAT_KEY);
            String lng = WLPreferences.loadStringPref(context, PREF_LNG_KEY);
            String location = locationText.getText().toString();
            saveStringPref(context, PREF_LOCATION_KEY, location);

            // Store selected category
            String category = getCheckedCategory(findViewById(R.id.categoryChipGroup));
            saveStringPref(context, Constants.PREF_CATEGORY_KEY + mAppWidgetId, category);

            progressBar.setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Refresh the widget
                    Timber.d("Configuring Widget: widgetId = %d, category = %s, location = %s, lat = %s, lng = %s", mAppWidgetId, category, location, lat, lng);
                    Intent refreshIntent = new Intent(context, WanderingWidget.class);
                    refreshIntent.setAction(Constants.WL_ACTION_WIDGET_CLICK);
                    refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    sendBroadcast(refreshIntent);

                    // Make sure we pass back the original appWidgetId
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);

                    WanderingWidget.sendRefreshBroadcast(WanderingWidgetConfigureActivity.this);
                    finish();
                }
            }, new Random().nextLong() % 1000 + 200);
        }
    };

    public WanderingWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.wandering_widget_configure);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        locationText = findViewById(R.id.location_text);
        locationText.setText(WLPreferences.loadStringPref(this, PREF_LOCATION_KEY));

        customSearchText = findViewById(R.id.custom_search_text);

        categoryRepo.getCategories().observe(this, this::initSearchUI);

        useMyLocationCheckbox = findViewById(R.id.useMyLocation_checkbox);
        useMyLocationCheckbox.setOnCheckedChangeListener(useMyLocationClickListener);

        progressBar = findViewById(R.id.progress_bar);
        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            //todo: why is this always off by 1?
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) + 1;
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    /**
     * Initializes the search UI and sets the category filtration options.
     * This function is idempotent as it clears the category views before
     * adding new ones provided by a LiveData object in {@link CategoryRepo}.
     *
     * @param categories
     */
    private void initSearchUI(List<WLCategory> categories) {
        ChipGroup cg = findViewById(R.id.categoryChipGroup);
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
        String lastSearch = WLPreferences.loadStringPref(this, Constants.PREF_CATEGORY_KEY + mAppWidgetId, Constants.DEFAULT_SEARCH_TERM);
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : Constants.PERMISSIONS) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), Constants.PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    storeLastKnownLocation();
                }
            }
        }
    }

    private Location getLastKnownLocation(LocationManager locationManager) {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(getClass().getSimpleName(), "Requesting location permissions");
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, Constants.PERMISSION_REQUEST_CODE);
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

    private void storeLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                Timber.d("Requesting location permissions");
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, Constants.PERMISSION_REQUEST_CODE);
                return;
            }
        }
        Timber.d("Location granted, grabbing last known location");
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Could not get location from GPS, please check your settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        Location location = getLastKnownLocation(locationManager);
        String cityState = LocationUtils.getCityStateFormattedStringFromLocation(WanderingWidgetConfigureActivity.this, location);
        Timber.d("Last known location: %s", cityState);
        if (cityState.length() == 0) {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, handlerThread.getLooper());
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, handlerThread.getLooper());
        } else {
            locationText.setText(cityState);

            // Try to use lat / lng if possible, fall back to city, state
            String lat = String.valueOf(location.getLatitude());
            String lng = String.valueOf(location.getLongitude());

            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LOCATION_KEY, cityState);
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LAT_KEY, lat);
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LNG_KEY, lng);
            Timber.d("Storing location: %s, lat=%f, lng=%f", cityState, location.getLatitude(), location.getLongitude());
        }
    }
}

