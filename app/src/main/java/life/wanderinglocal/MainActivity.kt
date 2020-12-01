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
import android.view.Window
import androidx.activity.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import life.wanderinglocal.databinding.ActivityMainBinding
import life.wanderinglocal.databinding.SearchLayoutBinding
import timber.log.Timber

class MainActivity : ComponentActivity() {
    // Firebase
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    // View binding
    private var mainBinding: ActivityMainBinding? = null
    private var searchBinding: SearchLayoutBinding? = null

    /**
     * Returns the search view and inflates it if it has not yet been created.
     *
     * @return View containing the search options.
     */
    // Views + ViewModels
    private var searchView: View? = null
        get() {
            if (field == null) {
                field = LayoutInflater.from(this).inflate(R.layout.search_layout, null)
           }
            return field
        }
    private var searchDialog: AlertDialog? = null
    private var yelpAdapter: WLTimeLineAdapter? = null
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFirebase()
        initUI()
        setContentView(mainBinding!!.root)
        initViewModel()
        initLocationServices()
        initAdmob()
    }

    override fun onPause() {
        super.onPause()
        if (getSearchDialog().isShowing) {
            getSearchDialog().dismiss()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
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

    private fun initViewModel() {
        Timber.d("Initializing view model...")
        with(viewModel){
            initializeRepo(this@MainActivity)
            setSearchingBy(WLPreferences.loadStringPref(this@MainActivity, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM))
            timeline?.observe(this@MainActivity, Observer { yelpData: List<WLTimelineEntry>? ->
                Timber.d("Timeline updated")
                yelpData?.let {
                    yelpAdapter?.setData(yelpData)
                    mainBinding?.progressBar?.visibility = View.GONE
                    mainBinding?.errorText?.visibility = if (yelpData.isEmpty()) View.VISIBLE else View.GONE
                }
            })
            searchingBy?.observe(this@MainActivity, Observer { s: WLCategory -> title = getString(R.string.app_name) + " - " + s.name })
            categories.observe(this@MainActivity, Observer { categories: List<WLCategory> -> initSearchUI(categories) })
        }
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM,
                viewModel.searchingBy?.value!!.name)
        mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)
        // Try to use last lat / lng if possible.
        val lat = WLPreferences.loadStringPref(this@MainActivity, Constants.PREF_LAT_KEY, null)
        val lng = WLPreferences.loadStringPref(this@MainActivity, Constants.PREF_LNG_KEY, null)
        if (lat != null && lng != null) {
            viewModel.setLocation(lat, lng)
        }
    }

    private fun initAdmob() {
        Timber.d("Initializing Admob...")
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        val adView: AdView = findViewById(R.id.adView)
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Timber.e(loadAdError.message)
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad failed to load")
                mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad clicked")
                mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad impression")
                mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }
        }
        adView.loadAd(adRequest)
    }

    private fun initLocationServices() {
        Timber.d("Initializing location services...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                Timber.d("Requesting location permissions")
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQUEST_CODE)
                return
            }
        }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = getLastKnownLocation(locationManager)
        if (location != null) {
            viewModel!!.setLocation(location.latitude.toString(), location.longitude.toString())
            viewModel!!.refresh()
        }
    }

    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Timber.d("Requesting location permissions")
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQUEST_CODE)
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
        Timber.d("Initializing UI...")
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        Timber.d("Initializing ProgressBar...")
        mainBinding!!.progressBar.visibility = View.VISIBLE
        Timber.d("Initializing FloatingActionBar...")
        mainBinding!!.fab.setOnClickListener { view: View? ->
            getSearchDialog().show()
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Search")
            mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
        Timber.d("Initializing RecyclerView...")
        yelpAdapter = WLTimeLineAdapter(this)
        mainBinding!!.recyclerView.adapter = yelpAdapter
        mainBinding!!.recyclerView.layoutManager = GridLayoutManager(this, 2)
        mainBinding!!.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                //todo replace with Google paging library
            }
        })
        Timber.d("Initializing search view...")
        searchBinding = searchView?.let { SearchLayoutBinding.bind(it) }
        searchBinding?.searchButton?.setOnClickListener { view: View? ->
            val checkedCategory = getCheckedCategory(searchBinding!!.categoryChipGroup)
            if (checkedCategory == null || checkedCategory.length == 0) return@setOnClickListener
            Timber.d("Searching for %s", checkedCategory)
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, checkedCategory)
            mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle)
            mainBinding?.progressBar?.visibility = View.VISIBLE
            WLPreferences.saveStringPref(this, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, checkedCategory)
            viewModel?.setSearchingBy(checkedCategory)
            viewModel?.refresh()
            getSearchDialog().dismiss()
        }

    }

    /**
     * Initializes the search UI and sets the category filtration options.
     * This function is idempotent as it clears the category views before
     * adding new ones provided by a LiveData object in [CategoryRepo].
     *
     * @param categories
     */
    private fun initSearchUI(categories: List<WLCategory>) {
        val cg: ChipGroup = searchView!!.findViewById(R.id.categoryChipGroup)
        cg.setOnCheckedChangeListener(object : ChipGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: ChipGroup, checkedId: Int) {
                group.setOnCheckedChangeListener(null)
                for (i in 0 until cg.childCount) {
                    val v = cg.getChildAt(i)
                    if (v is Chip) {
                        val c = v
                        if (c.id != checkedId) {
                            c.isChecked = false
                        }
                    }
                }
                group.setOnCheckedChangeListener(this)
            }
        })

        // Clear chip children before adding categories.
        for (i in 0 until cg.childCount) {
            val v = cg.getChildAt(i)
            if (v is Chip) {
                cg.removeViewAt(i)
            }
        }
        val lastSearch = WLPreferences.loadStringPref(this, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM)
        for (category in categories) {
            val c = Chip(this)
            c.text = category.name
            c.isCheckable = true
            if (category.name == lastSearch) {
                c.isChecked = true
            }
            cg.addView(c)
        }
    }

    /**
     * Inflates / returns the [AlertDialog] containing the search view.
     *
     * @return [AlertDialog]
     */
    private fun getSearchDialog(): AlertDialog {
        if (searchDialog == null) {
            searchDialog = AlertDialog.Builder(this).setView(searchView).create()
            searchDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            searchDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        return searchDialog!!
    }

    private fun getCheckedCategory(cg: ChipGroup?): String? {
        if (cg == null) return null
        for (i in 0 until cg.childCount) {
            val v = cg.getChildAt(i)
            if (v is Chip) {
                val c = v
                if (c.isChecked) {
                    return c.text.toString()
                }
            }
        }
        return null
    }

    companion object {
        private const val REQUEST_CODE = 71
    }
}