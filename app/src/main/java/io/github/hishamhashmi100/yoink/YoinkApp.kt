package io.github.hishamhashmi100.yoink

import android.app.Application
import android.content.Context
import io.github.hishamhashmi100.yoink.data.AppDatabase
import io.github.hishamhashmi100.yoink.data.DownloadRepository
import io.github.hishamhashmi100.yoink.data.SettingsStore
import io.github.hishamhashmi100.yoink.download.Engine
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.download.UpdateWorker
import io.github.hishamhashmi100.yoink.update.DailyCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class YoinkApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.build(this) }
    val repository by lazy { DownloadRepository(this, database.downloadDao()) }
    val settings by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        val startedAt = System.currentTimeMillis()
        appScope.launch {
            // Unpacks Python on first run, so the share sheet doesn't have to wait for it later.
            runCatching { Engine.ensureReady(this@YoinkApp) }
            repository.reconcile(getString(R.string.error_interrupted), createdBefore = startedAt)
            if (!settings.firstUpdateCheckDone.first()) UpdateWorker.enqueue(this@YoinkApp)
        }
        DailyCheckWorker.schedule(this)
    }
}

val Context.yoink: YoinkApp get() = applicationContext as YoinkApp
