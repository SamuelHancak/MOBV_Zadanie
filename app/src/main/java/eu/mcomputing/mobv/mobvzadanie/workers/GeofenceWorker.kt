package eu.mcomputing.mobv.mobvzadanie.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.mcomputing.mobv.mobvzadanie.data.PreferenceData
import eu.mcomputing.mobv.mobvzadanie.utils.WithinTimeHelper.isWithinTimeRange


class GeofenceWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        PreferenceData.getInstance().putTimeSharing(applicationContext, true)
        if (isWithinTimeRange()) {
            return Result.success()
        }

        return Result.failure()
    }
}