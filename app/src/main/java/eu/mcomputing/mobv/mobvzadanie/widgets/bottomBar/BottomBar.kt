package eu.mcomputing.mobv.mobvzadanie.widgets.bottomBar

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import eu.mcomputing.mobv.mobvzadanie.R
import eu.mcomputing.mobv.mobvzadanie.data.PreferenceData


class BottomBar : ConstraintLayout {
    private var active = -1

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    fun setActive(index: Int) {
        active = index
    }

    fun init() {
        val layout =
            LayoutInflater.from(context)
                .inflate(R.layout.widget_bottom_bar, this, false)
        addView(layout)

        layout.findViewById<ImageView>(R.id.map).setOnClickListener {
            if (active != MAP) {
                Log.d("BottomBar", PreferenceData.getInstance().getSharing(context).toString())
                if (PreferenceData.getInstance().getSharing(context)) {
                    it.findNavController().navigate(R.id.action_to_map)
                } else {
                    it.findNavController().navigate(R.id.action_to_feed_location)
                }
            }
        }
        layout.findViewById<ImageView>(R.id.feed).setOnClickListener {
            if (active != FEED) {
                if (PreferenceData.getInstance().getSharing(context)) {
                    it.findNavController().navigate(R.id.action_to_feed)
                } else {
                    it.findNavController().navigate(R.id.action_to_feed_location)
                }
            }
        }
        layout.findViewById<ImageView>(R.id.profile).setOnClickListener {
            if (active != PROFILE) {
                it.findNavController().navigate(R.id.action_to_profile)
            }
        }
    }


    companion object {
        const val MAP = 0
        const val FEED = 1
        const val PROFILE = 2
    }
}