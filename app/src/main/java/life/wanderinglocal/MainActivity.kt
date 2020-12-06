package life.wanderinglocal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.rxjava3.functions.Consumer
import life.wanderinglocal.databinding.ActivityMainBinding
import life.wanderinglocal.databinding.SearchLayoutBinding
import life.wanderinglocal.databinding.TimelineLayoutBinding
import life.wanderinglocal.fragment.TimelineFragment
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    // Firebase
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    // View binding
    private var mainBinding: ActivityMainBinding? = null
    private val viewModel: TimelineViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFirebase()
        initViewModel()
        initUI()
        setContentView(mainBinding?.root)
        supportFragmentManager.commit {
            replace<TimelineFragment>(R.id.fragment_container, Constants.TIMELINE_FRAGMENT_TAG)
        }
        initLocationServices()
    }

    fun initViewModel() {
        viewModel.initializeRepo(this@MainActivity)
        viewModel.searchingBy?.observe(this@MainActivity, Observer { s: WLCategory ->
            supportActionBar?.title = getString(R.string.app_name) + " - " + s.name
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
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
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = getLastKnownLocation(locationManager)
        if (location != null) {
            viewModel.setLocation(location.latitude.toString(), location.longitude.toString())
            viewModel.refresh()
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
    }
}