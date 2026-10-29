package io.github.hishamhashmi100.yoink.stats

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Sends waiting usage data a minute after the first new event, so events go together in one request. */
class StatsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Batch after batch until nothing is left; if the website can't be reached, try again later.
        while (true) {
            val left = runCatching { Stats.send(applicationContext) }.getOrNull() ?: return Result.retry()
            if (left == 0) return Result.success()
        }
    }

    companion object {
        private const val NAME = "stats-send"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<StatsWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
