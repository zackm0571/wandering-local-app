package life.wanderinglocal.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.rxjava3.functions.Consumer
import life.wanderinglocal.*
import life.wanderinglocal.R
import life.wanderinglocal.databinding.SearchLayoutBinding
import life.wanderinglocal.databinding.TimelineLayoutBinding
import timber.log.Timber

class TimelineFragment : Fragment() {
    private lateinit var binding: TimelineLayoutBinding
    private var searchBinding: SearchLayoutBinding? = null
    private lateinit var searchView: View
    private var searchDialog: AlertDialog? = null
    private val viewModel: TimelineViewModel by activityViewModels()
    private var timelineAdapter: WLTimeLineAdapter? = null
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TimelineLayoutBinding.inflate(inflater)
        searchView = inflater.inflate(R.layout.search_layout, null)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(it)
        }
        Timber.d("Initializing UI...")
        Timber.d("Initializing ProgressBar...")
        binding.progressBar.visibility = View.VISIBLE
        Timber.d("Initializing FloatingActionBar...")
        binding.fab.setOnClickListener {
            getSearchDialog().show()
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Search")
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
        Timber.d("Initializing RecyclerView...")
        timelineAdapter = context?.let { WLTimeLineAdapter(it) }
        timelineAdapter?.itemClickedSubject?.subscribe(Consumer {
            parentFragmentManager.commit {
                parentFragmentManager.findFragmentByTag(Constants.TIMELINE_FRAGMENT_TAG)?.let {
                    hide(it)
                }

                add<EntryDetailFragment>(R.id.fragment_container, Constants.TIMELINE_DETAIL_FRAGMENT_TAG)
                addToBackStack(Constants.TIMELINE_FRAGMENT_TAG)
                viewModel.selected.value = it
            }
        })
        binding.recyclerView.adapter = timelineAdapter
        binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                //todo replace with Google paging library
            }
        })
        Timber.d("Initializing search view...")
        searchBinding = SearchLayoutBinding.bind(searchView)
        searchBinding?.searchButton?.setOnClickListener {
            val checkedCategory = getCheckedCategory(searchBinding!!.categoryChipGroup)
            if (checkedCategory == null || checkedCategory.length == 0) return@setOnClickListener
            Timber.d("Searching for %s", checkedCategory)
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, checkedCategory)
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle)
            binding.progressBar.visibility = View.VISIBLE
            WLPreferences.saveStringPref(context, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, checkedCategory)
            viewModel.setSearchingBy(checkedCategory)
            viewModel.refresh()
            getSearchDialog().dismiss()
        }

        with(viewModel) {
            setSearchingBy(WLPreferences.loadStringPref(context, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, life.wanderinglocal.Constants.DEFAULT_SEARCH_TERM))
            categories.observe(viewLifecycleOwner, Observer { categories: List<WLCategory> -> initSearchUI(categories) })
            timeline.observe(viewLifecycleOwner, Observer { yelpData: List<WLTimelineEntry>? ->
                Timber.d("Timeline updated")
                yelpData?.let {
                    timelineAdapter?.setData(yelpData)
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.errorText.visibility = if (yelpData.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
            })
            location.observe(viewLifecycleOwner, Observer {
                refresh()
            })
        }
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM,
                viewModel.searchingBy.value?.name)
        // Try to use last lat / lng if possible.
        initAdmob()
    }

    override fun onPause() {
        super.onPause()
        if (getSearchDialog().isShowing) {
            getSearchDialog().dismiss()
        }
    }

    private fun initAdmob() {
        Timber.d("Initializing Admob...")
        MobileAds.initialize(context)
        val adRequest = AdRequest.Builder().build()
        val adView: AdView = binding.adView
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Timber.e(loadAdError.message)
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad failed to load")
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad clicked")
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT, "Ad impression")
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }
        }
        adView.loadAd(adRequest)
    }

    // Search View
    /**
     * Initializes the search UI and sets the category filtration options.
     * This function is idempotent as it clears the category views before
     * adding new ones provided by a LiveData object in [CategoryRepo].
     *
     * @param categories
     */
    private fun initSearchUI(categories: List<WLCategory>) {
        val cg: ChipGroup = searchView.findViewById(R.id.categoryChipGroup)
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
        val lastSearch = WLPreferences.loadStringPref(context, Constants.PREF_LAST_SEARCHED_CATEGORY_KEY, Constants.DEFAULT_SEARCH_TERM)
        for (category in categories) {
            val c = Chip(context)
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
            searchDialog = context?.let { AlertDialog.Builder(it).setView(searchView).create() }
            searchDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            searchDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
}