package io.github.hishamhashmi100.yoink.stats

import android.content.Context
import android.os.Build
import io.github.hishamhashmi100.yoink.BuildConfig
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

/**
 * Usage data, only from people who said yes. Events wait in a small file and go to the website's
 * /api/events in batches (StatsWorker), so they survive being offline and don't cost a request each.
 * The database keeps each build's data apart (functions/api/events.js). Builds without STATS_BUILD
 * (the real app, for now) collect nothing.
 */
object Stats {

    private const val ENDPOINT = "https://yoink.download/api"
    private const val MAX_BATCH = 200
    private const val KEY_CONSENT = "consent"
    private const val KEY_ID = "install_id"

    private val lock = Any()

    /** This build collects usage data (when the user agrees). */
    val available: Boolean get() = BuildConfig.STATS_BUILD.isNotEmpty()

    private fun prefs(context: Context) = context.getSharedPreferences("stats", Context.MODE_PRIVATE)

    private fun queue(context: Context) = File(context.filesDir, "stats-queue.jsonl")

    /** Yes, no, or null if the user hasn't been asked yet. */
    fun consent(context: Context): Boolean? =
        prefs(context).takeIf { it.contains(KEY_CONSENT) }?.getBoolean(KEY_CONSENT, false)

    fun setConsent(context: Context, yes: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONSENT, yes).apply()
        if (yes) record(context, "opted_in") else synchronized(lock) { queue(context).delete() }
    }

    /** A random ID for this install: not tied to the phone, a name or a Google account. */
    fun installId(context: Context): String = synchronized(lock) {
        prefs(context).getString(KEY_ID, null) ?: UUID.randomUUID().toString().also {
            prefs(context).edit().putString(KEY_ID, it).apply()
        }
    }

    /** Notes an event (if the user agreed) and schedules sending. Values are strings, numbers or booleans. */
    fun record(context: Context, name: String, data: Map<String, Any?> = emptyMap()) {
        if (!available || consent(context) != true) return
        val app = context.yoink
        val line = JSONObject()
            .put("name", name)
            .put("at", System.currentTimeMillis())
            .put("data", JSONObject(data.filterValues { it != null }))
            .toString()
        app.appScope.launch(Dispatchers.IO) {
            synchronized(lock) { queue(app).appendText(line + "\n") }
            StatsWorker.schedule(app)
        }
    }

    /** Sends one batch of what's waiting. Returns how many events are still waiting, or null if it couldn't send. */
    suspend fun send(context: Context): Int? = withContext(Dispatchers.IO) {
        if (!available || consent(context) != true) return@withContext 0
        val lines = synchronized(lock) { queue(context).takeIf { it.exists() }?.readLines().orEmpty() }
            .filter { it.isNotBlank() }
            .take(MAX_BATCH)
        if (lines.isEmpty()) return@withContext 0
        val events = JSONArray().apply { lines.forEach { line -> runCatching { put(JSONObject(line)) } } }
        val body = JSONObject()
            .put("id", installId(context))
            .put("build", BuildConfig.STATS_BUILD)
            .put("app", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            .put("android", Build.VERSION.SDK_INT)
            .put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("language", Locale.getDefault().toLanguageTag())
            .put("events", events)
        if (!post("$ENDPOINT/events", body)) return@withContext null
        // Only events are ever added at the end, so the sent ones are still the first lines.
        synchronized(lock) {
            val file = queue(context)
            val rest = file.readLines().filter { it.isNotBlank() }.drop(lines.size)
            if (rest.isEmpty()) file.delete() else file.writeText(rest.joinToString("\n", postfix = "\n"))
            rest.size
        }
    }

    /** "Delete my data": removes this install's events from the database and starts over with a new ID. */
    suspend fun forget(context: Context): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("id", installId(context)).put("build", BuildConfig.STATS_BUILD)
        if (!post("$ENDPOINT/forget", body)) return@withContext false
        synchronized(lock) {
            queue(context).delete()
            prefs(context).edit().remove(KEY_ID).apply()
        }
        true
    }

    private fun post(url: String, body: JSONObject): Boolean = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)
}
