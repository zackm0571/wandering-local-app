package life.wanderinglocal

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.*
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import life.wanderinglocal.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : FragmentActivity() {
    // Firebase
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    // Location
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // View binding
    private var mainBinding: ActivityMainBinding? = null
    private val viewModel: TimelineViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        setContentView(mainBinding?.root)
        initFirebase()
        initViewModel()
        initLocationServices()
    }

    fun initViewModel() {
        viewModel.searchingBy.value = WLCategory(WLPreferences.loadStringPref(this@MainActivity, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM))
        viewModel.searchingBy.observe(this@MainActivity, Observer { s: WLCategory ->
            actionBar?.title = getString(R.string.app_name) + " - " + s.name
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permissions accepted")
                    initLocationServices()
                }
            }
        }
    }

    private fun initFirebase() {
        Timber.d("Initializing Firebase...")
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseCrashlytics.getInstance().sendUnsentReports()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, javaClass.simpleName)
        mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)
    }

    private fun initLocationServices() {
        Timber.d("Initializing location services...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                Timber.d("Requesting location permissions")
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, Constants.PERMISSION_REQUEST_CODE)
                return
            }
        }
        fusedLocationProvider = FusedLocationProviderClient(this@MainActivity)
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        getLastKnownLocation(locationManager)?.let {
            viewModel.setLocation(it.latitude, it.longitude)
        }
        val locationRequest = LocationRequest.create()?.apply {
            interval = 100000
            fastestInterval = 100000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = locationRequest?.let {
            LocationSettingsRequest.Builder()
                    .addLocationRequest(it)
        }

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder?.build())
        task.addOnSuccessListener {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    var bestLocation: Location? = null
                    for (location in locationResult.locations) {
                        if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                            // Found best last known location: %s", l);
                            bestLocation = location
                        }
                    }
                    bestLocation?.let {
                        viewModel.setLocation(it.latitude, it.longitude)
                    }
                }
            }
            fusedLocationProvider.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper())
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MainActivity,
                            Constants.CHECK_LOCATION_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Timber.d("Requesting location permissions")
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, Constants.PERMISSION_REQUEST_CODE)
                return null
            }
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                // Found best last known location: %s", l);
                bestLocation = l
            }
        }
        return bestLocation
    }

    private fun initUI() {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setActionBar(mainBinding?.toolbar)
        actionBar?.setDisplayShowHomeEnabled(true)
    }
}