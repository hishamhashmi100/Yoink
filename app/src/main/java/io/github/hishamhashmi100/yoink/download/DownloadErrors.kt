package io.github.hishamhashmi100.yoink.download

import android.content.Context
import io.github.hishamhashmi100.yoink.R
import java.io.IOException

/** Turns yt-dlp's stderr into something a person can act on. */
object DownloadErrors {

    private val rules: List<Pair<Regex, Int>> = listOf(
        Regex("private video", RegexOption.IGNORE_CASE) to R.string.error_private,
        Regex("confirm your age|age[- ]restricted|inappropriate for some users", RegexOption.IGNORE_CASE) to R.string.error_age,
        Regex("members[- ]only|join this channel", RegexOption.IGNORE_CASE) to R.string.error_members,
        Regex("not a bot", RegexOption.IGNORE_CASE) to R.string.error_bot,
        Regex("is live|live event will begin|premieres in", RegexOption.IGNORE_CASE) to R.string.error_live,
        Regex("video unavailable|has been removed|no longer available|account associated with this video has been terminated|not available in your country", RegexOption.IGNORE_CASE) to R.string.error_unavailable,
        Regex("requested format is not available", RegexOption.IGNORE_CASE) to R.string.error_format,
        Regex("no space left|ENOSPC", RegexOption.IGNORE_CASE) to R.string.error_storage,
        Regex("HTTP Error 403|Forbidden", RegexOption.IGNORE_CASE) to R.string.error_forbidden,
        Regex(
            "unable to resolve|failed to resolve|getaddrinfo|name or service not known|timed out|connection (reset|refused|aborted)|network is unreachable|temporary failure in name resolution|ssl",
            RegexOption.IGNORE_CASE,
        ) to R.string.error_network,
    )

    fun readable(context: Context, error: Throwable): String {
        val text = generateSequence(error) { it.cause }.mapNotNull { it.message }.joinToString("\n")
        rules.firstOrNull { (regex, _) -> regex.containsMatchIn(text) }?.let { return context.getString(it.second) }
        if (error is IOException && text.isBlank()) return context.getString(R.string.error_network)

        // Fall back to yt-dlp's own last "ERROR:" line, without the "[youtube] <id>:" prefix.
        val last = text.lineSequence().lastOrNull { "ERROR:" in it }
            ?.substringAfter("ERROR:")
            ?.replace(Regex("""^\s*\[[^\]]+]\s*[\w-]+:\s*"""), "")
            ?.trim()
        return last?.takeIf { it.isNotEmpty() }?.take(300) ?: context.getString(R.string.error_generic)
    }
}
