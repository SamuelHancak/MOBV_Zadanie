package eu.mcomputing.mobv.mobvzadanie.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WithinTimeHelper {
    fun isWithinTimeRange(): Boolean {
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = dateFormat.format(currentTime)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTimeDate = sdf.parse(formattedTime)
        val startTimeDate = sdf.parse("10:00")
        val endTimeDate = sdf.parse("17:00")

        return currentTimeDate in startTimeDate..endTimeDate
    }
}