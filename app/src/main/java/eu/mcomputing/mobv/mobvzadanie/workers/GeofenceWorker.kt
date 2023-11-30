package eu.mcomputing.mobv.mobvzadanie.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.mcomputing.mobv.mobvzadanie.data.PreferenceData
import eu.mcomputing.mobv.mobvzadanie.utils.WithinTimeHelper.isWithinTimeRange


class GeofenceWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        PreferenceData.getInstance().putTimeSharing(applicationContext, true)
        if (isWithinTimeRange()) {
            Log.d("GeofenceWorker", "Within time range")
            return Result.success()
        }
        Log.d("GeofenceWorker", "Not within time range")

        return Result.failure()
    }
}