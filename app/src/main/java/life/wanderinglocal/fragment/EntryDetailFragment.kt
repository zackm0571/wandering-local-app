package life.wanderinglocal.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.firebase.analytics.FirebaseAnalytics
import life.wanderinglocal.*
import life.wanderinglocal.R
import life.wanderinglocal.databinding.TimelineDetailLayoutBinding
import life.wanderinglocal.databinding.TimelineLayoutBinding
import timber.log.Timber

class EntryDetailFragment : Fragment() {
    private val viewModel: TimelineViewModel by activityViewModels()
    private lateinit var binding: TimelineDetailLayoutBinding
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TimelineDetailLayoutBinding.inflate(inflater)
        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onDestroyView() {
        val activity = activity as? MainActivity
        activity?.actionBar?.setDisplayHomeAsUpEnabled(false)
        setHasOptionsMenu(false)
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val activity = activity as? MainActivity
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(it)
        }
        viewModel.selected.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.titleDetail.text = it.businessName
                binding.addressDetail.text = it.locationString
                it.isFavorite = ServiceLocator.favoritesRepo.favorites.contains(it.id)
                setFavoriteFabImageState()
                context?.let { it1 -> Glide.with(it1).load(it.imageUrl).into(binding.logoDetail) }
            }
        })
        binding.favoriteFab.setOnClickListener {
            viewModel.selected.value?.let {
                it.isFavorite = it.isFavorite.not()
                when (it.isFavorite) {
                    true -> ServiceLocator.favoritesRepo.favorites.add(it.id)
                    false -> ServiceLocator.favoritesRepo.favorites.remove(it.id)
                }
                WLPreferences.putStringSetPref(context, Constants.PREF_IS_FAVORITE_KEY, ServiceLocator.favoritesRepo.favorites)
                setFavoriteFabImageState()
            }
        }
        initAdmob()
    }

    private fun setFavoriteFabImageState() {
        when (viewModel.selected.value?.isFavorite) {
            true -> binding.favoriteFab.setImageResource(R.drawable.favorite)
            false -> binding.favoriteFab.setImageResource(R.drawable.favorite_border)
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
}