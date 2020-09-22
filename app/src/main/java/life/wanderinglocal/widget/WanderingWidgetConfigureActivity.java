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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import life.wanderinglocal.Constants;
import life.wanderinglocal.LocationUtils;
import life.wanderinglocal.R;
import life.wanderinglocal.TimelineRepo;
import life.wanderinglocal.WLCategory;
import life.wanderinglocal.WLPreferences;

import static life.wanderinglocal.Constants.PREF_LAT_KEY;
import static life.wanderinglocal.Constants.PREF_LNG_KEY;
import static life.wanderinglocal.Constants.PREF_LOCATION_KEY;
import static life.wanderinglocal.WLPreferences.saveStringPref;

/**
 * The configuration screen for the {@link WanderingWidget WanderingWidget} AppWidget.
 */
public class WanderingWidgetConfigureActivity extends Activity {
    TimelineRepo repo;
    HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName() + "_" + "thread");
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText locationText, customSearchText;
    CheckBox useMyLocationCheckbox;
    RadioGroup categoriesRadioGroup;
    ProgressBar progressBar;
    EditText mAppWidgetText;
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            String cityState = LocationUtils.getCityStateFormattedStringFromLocation(WanderingWidgetConfigureActivity.this, location);
            locationText.setText(cityState);
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LOCATION_KEY, cityState);
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LAT_KEY, String.valueOf(location.getLatitude()));
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LNG_KEY, String.valueOf(location.getLongitude()));
            Log.d(getClass().getSimpleName(), String.format("Storing location: %s, lat=%d, lng=%d", cityState, location.getLatitude(), location.getLongitude()));
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
    RadioGroup.OnCheckedChangeListener onCategoryChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int id) {
            radioGroup.setOnCheckedChangeListener(null);
            radioGroup.clearCheck();
            radioGroup.check(id);
            RadioButton selectedButton = findViewById(id);

            Log.d(getClass().getSimpleName(), String.format("Selected category: %s", selectedButton.getText().toString()));

            if (selectedButton.getText().equals(getString(R.string.custom_search_term))) {
                customSearchText.setVisibility(View.VISIBLE);
            } else {
                customSearchText.setVisibility(View.GONE);
            }

            radioGroup.setOnCheckedChangeListener(this);
        }
    };
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = WanderingWidgetConfigureActivity.this;

            String location = locationText.getText().toString();
            saveStringPref(context, PREF_LOCATION_KEY, location);

            // Store selected category
            RadioButton selectedCategory = findViewById(categoriesRadioGroup.getCheckedRadioButtonId());
            String category = (customSearchText.getVisibility() == View.VISIBLE)
                    ? customSearchText.getText().toString() : selectedCategory.getText().toString();
            saveStringPref(context, Constants.PREF_CATEGORY_KEY, category);

            Log.d(getClass().getSimpleName(), String.format("Widget category = %s, location = %s", category, location));
            // Try to use lat / lng if possible, fall back to city, state
            String lat = WLPreferences.loadStringPref(context, PREF_LAT_KEY);
            String lng = WLPreferences.loadStringPref(context, PREF_LNG_KEY);

            if (lat == null || lng == null) {
                repo.setLocation(location);
            } else {
                repo.setLocation(lat, lng);
            }
            repo.setSearchBy(category);
            repo.search();
            progressBar.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);

                // Refresh the widget
                Intent refreshIntent = new Intent(context, WanderingWidget.class);
                refreshIntent.setAction(Constants.WL_ACTION_WIDGET_CLICK);
                refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                sendBroadcast(refreshIntent);

                finish();
            }, 1000);
            v.setEnabled(false);
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
        mAppWidgetText = (EditText) findViewById(R.id.appwidget_text);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        locationText = findViewById(R.id.location_text);
        locationText.setText(WLPreferences.loadStringPref(this, PREF_LOCATION_KEY));

        customSearchText = findViewById(R.id.custom_search_text);

        useMyLocationCheckbox = findViewById(R.id.useMyLocation_checkbox);
        categoriesRadioGroup = findViewById(R.id.category_group);
        categoriesRadioGroup.setOnCheckedChangeListener(onCategoryChangedListener);
        useMyLocationCheckbox.setOnCheckedChangeListener(useMyLocationClickListener);
        progressBar = findViewById(R.id.progress_bar);
        repo = new TimelineRepo(this);
        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
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

    private void storeLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                Log.d(getClass().getSimpleName(), "Requesting location permissions");
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, Constants.PERMISSION_REQUEST_CODE);
                return;
            }
        }
        Log.d(getClass().getSimpleName(), "Location granted, grabbing last known location");
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Could not get location from GPS, please check your settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        String cityState = LocationUtils.getCityStateFormattedStringFromLocation(WanderingWidgetConfigureActivity.this, location);
        Log.d(getClass().getSimpleName(), String.format("Last known location: %s", cityState));
        if (cityState.length() == 0) {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, handlerThread.getLooper());
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, handlerThread.getLooper());
        } else {
            locationText.setText(cityState);

            // Try to use lat / lng if possible, fall back to city, state
            String lat = String.valueOf(location.getLatitude());
            String lng = String.valueOf(location.getLongitude());

            repo.setLocation(lat, lng);

            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LOCATION_KEY, cityState);
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LAT_KEY, String.valueOf(location.getLatitude()));
            saveStringPref(WanderingWidgetConfigureActivity.this, PREF_LNG_KEY, String.valueOf(location.getLongitude()));
            Log.d(getClass().getSimpleName(), String.format("Storing location: %s, lat=%f, lng=%f", cityState, location.getLatitude(), location.getLongitude()));
        }
    }
}

