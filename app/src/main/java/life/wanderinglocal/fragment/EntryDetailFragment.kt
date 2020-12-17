package life.wanderinglocal.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.bumptech.glide.Glide
import life.wanderinglocal.Constants
import life.wanderinglocal.R
import life.wanderinglocal.TimelineViewModel
import life.wanderinglocal.databinding.TimelineDetailLayoutBinding
import life.wanderinglocal.databinding.TimelineLayoutBinding

class EntryDetailFragment : Fragment() {
    private val viewModel: TimelineViewModel by activityViewModels()
    private lateinit var binding: TimelineDetailLayoutBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TimelineDetailLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selected.value?.let {
            binding.titleDetail.text = it.businessName
            context?.let { it1 -> Glide.with(it1).load(it.imageUrl).into(binding.logoDetail) }
        }
        binding.backButton.setOnClickListener {
            parentFragmentManager.commit {
                replace<TimelineFragment>(R.id.fragment_container, Constants.TIMELINE_FRAGMENT_TAG)
            }
        }
    }
}