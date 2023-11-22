package eu.mcomputing.mobv.mobvzadanie.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import eu.mcomputing.mobv.mobvzadanie.R
import eu.mcomputing.mobv.mobvzadanie.data.PreferenceData
import eu.mcomputing.mobv.mobvzadanie.databinding.FragmentFeedLocationBinding
import eu.mcomputing.mobv.mobvzadanie.widgets.bottomBar.BottomBar

class FeedLocationFragment : Fragment(R.layout.fragment_feed_location) {
    private var binding: FragmentFeedLocationBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentFeedLocationBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
        }.also { bnd ->
            bnd.bottomBar.setActive(BottomBar.FEED)

            bnd.enableLocationButton.setOnClickListener {
                PreferenceData.getInstance().putSharing(requireContext(), true)
                it.findNavController().navigate(R.id.action_to_profile)
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
