package io.github.hishamhashmi100.yoink.update

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class Release(
    val version: String,
    val pageUrl: String,
    val apkName: String,
    val apkUrl: String,
    val checksumsUrl: String?,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: Release, val file: File) : UpdateState
    data class Failed(val release: Release?, val message: String) : UpdateState
}

/**
 * Yoink isn't on Google Play, so it updates itself from GitHub Releases: check the latest release,
 * download the APK for this phone's CPU, verify it against SHA256SUMS.txt, and hand it to Android's
 * installer. Android always asks the user to confirm.
 */
object AppUpdater {

    private const val LATEST_RELEASE = "https://api.github.com/repos/hishamhashmi100/Yoink/releases/latest"
    private const val RECHECK_AFTER_MS = 6 * 60 * 60 * 1000L

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val checkLock = Mutex()
    @Volatile private var lastCheck = 0L

    /** Debug builds have a different app ID, so a release APK would install next to them, not over them. */
    fun canSelfUpdate(context: Context): Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0

    fun currentVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty().substringBefore('-')

    /** Returns the newer release, if there is one. Without [force], checks at most every few hours. */
    suspend fun check(context: Context, force: Boolean = false): Release? = checkLock.withLock {
        when (val current = _state.value) {
            is UpdateState.Downloading -> return current.release
            is UpdateState.ReadyToInstall -> return current.release
            is UpdateState.Available -> if (!force) return current.release
            else -> Unit
        }
        if (!force && _state.value == UpdateState.UpToDate && System.currentTimeMillis() - lastCheck < RECHECK_AFTER_MS) return null

        _state.value = UpdateState.Checking
        try {
            val release = withContext(Dispatchers.IO) { fetchLatest() }
            lastCheck = System.currentTimeMillis()
            val newer = release?.takeIf { isNewer(it.version, currentVersion(context)) }
            _state.value = if (newer != null) UpdateState.Available(newer) else UpdateState.UpToDate
            newer
        } catch (e: CancellationException) {
            _state.value = UpdateState.Idle
            throw e
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(null, context.getString(R.string.update_check_failed))
            null
        }
    }

    /** Downloads the update in the background; the installer opens when it's done, if Yoink is on screen. */
    fun download(context: Context) {
        val release = when (val s = _state.value) {
            is UpdateState.Available -> s.release
            is UpdateState.Failed -> s.release
            else -> null
        } ?: return
        val app = context.yoink
        _state.value = UpdateState.Downloading(release, 0f)
        app.appScope.launch(Dispatchers.IO) {
            try {
                val file = fetchApk(app.cacheDir, release) { _state.value = UpdateState.Downloading(release, it) }
                _state.value = UpdateState.ReadyToInstall(release, file)
                withContext(Dispatchers.Main) { install(app) }
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(release, app.getString(R.string.update_download_failed))
            }
        }
    }

    /**
     * Opens Android's installer for a downloaded update. The first time, Android asks the user to allow
     * Yoink to install apps; then they tap Install again. Returns false if that permission is still missing.
     */
    fun install(context: Context): Boolean {
        val ready = _state.value as? UpdateState.ReadyToInstall ?: return false
        if (!context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settings) }
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", ready.file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private fun fetchLatest(): Release? {
        val connection = open(LATEST_RELEASE)
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return null // No release yet.
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets")
            val urls = buildMap {
                for (i in 0 until (assets?.length() ?: 0)) {
                    val asset = assets!!.getJSONObject(i)
                    put(asset.getString("name"), asset.getString("browser_download_url"))
                }
            }
            // Same names as on the website: yoink-arm64-v8a.apk, yoink-armeabi-v7a.apk, yoink-universal.apk.
            val apkName = Build.SUPPORTED_ABIS.map { "yoink-$it.apk" }.firstOrNull { it in urls }
                ?: "yoink-universal.apk".takeIf { it in urls }
                ?: return null
            return Release(
                version = json.getString("tag_name").trimStart('v', 'V'),
                pageUrl = json.optString("html_url"),
                apkName = apkName,
                apkUrl = urls.getValue(apkName),
                checksumsUrl = urls["SHA256SUMS.txt"],
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchApk(cacheDir: File, release: Release, onProgress: (Float) -> Unit): File {
        val dir = File(cacheDir, "updates")
        dir.deleteRecursively()
        dir.mkdirs()
        val file = File(dir, release.apkName)
        val digest = MessageDigest.getInstance("SHA-256")

        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong
            var done = 0L
            var reported = -1
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != reported) {
                                reported = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        // The release lists SHA-256 checksums; a mismatch means a broken or tampered download.
        release.checksumsUrl?.let { url ->
            val sums = open(url).let { c -> try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() } }
            val expected = sums.lineSequence()
                .map { it.trim().split(Regex("\\s+")) }
                .firstOrNull { it.size >= 2 && it.last().removePrefix("*") == release.apkName }
                ?.first()
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (expected != null && !expected.equals(actual, ignoreCase = true)) {
                file.delete()
                throw IOException("Checksum mismatch")
            }
        }
        return file
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Yoink")
        }

    /** Compares dotted versions like 1.10.0 and 1.9.2 by number. */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }.let { it + List(3) { 0 } }.take(3)
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until 3) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }
}
