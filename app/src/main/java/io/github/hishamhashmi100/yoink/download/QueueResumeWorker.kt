package io.github.hishamhashmi100.yoink.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.hishamhashmi100.yoink.yoink
import java.util.concurrent.TimeUnit

/** Carries on with the waiting playlist videos once a pause for YouTube's limit is over (see DownloadRepository). */
class QueueResumeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        applicationContext.yoink.repository.resumeQueue()
        return Result.success()
    }

    companion object {
        private const val NAME = "queue-resume"

        fun schedule(context: Context, minutes: Long) {
            val request = OneTimeWorkRequestBuilder<QueueResumeWorker>()
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
