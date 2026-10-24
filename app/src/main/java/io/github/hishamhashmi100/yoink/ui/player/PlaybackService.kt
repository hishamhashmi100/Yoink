package io.github.hishamhashmi100.yoink.ui.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.ui.MainActivity
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.launch

/** Owns the player, so audio keeps going with the screen off or in another app. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    private val repeatCommand = SessionCommand(ACTION_REPEAT, Bundle.EMPTY)

    // Adds the repeat button to the system media player (notification and lock screen).
    private val callback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo) =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(repeatCommand).build(),
                )
                .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != ACTION_REPEAT) return super.onCustomCommand(session, controller, customCommand, args)
            val player = session.player
            val next = nextRepeatMode(player.repeatMode)
            player.repeatMode = next
            // Remembered the same way as the in-app buttons: MP3s and videos each keep their own.
            val app = yoink
            val audio = player.currentMediaItem?.mediaId?.startsWith("audio:") == true
            app.appScope.launch { if (audio) app.settings.setAudioRepeat(next) else app.settings.setVideoRepeat(next) }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_PLAYER, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(callback)
            .setMediaButtonPreferences(listOf(repeatButton(player.repeatMode)))
            .build()
        player.addListener(object : Player.Listener {
            // Keep the button's icon in step, whichever control changed the mode.
            override fun onRepeatModeChanged(repeatMode: Int) {
                session?.setMediaButtonPreferences(listOf(repeatButton(repeatMode)))
            }
        })
    }

    private fun repeatButton(mode: Int): CommandButton {
        val (icon, label) = when (mode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_media_repeat_one to R.string.player_repeat_one
            Player.REPEAT_MODE_ALL -> R.drawable.ic_media_repeat_all to R.string.player_repeat_all
            else -> R.drawable.ic_media_repeat_off to R.string.player_repeat_off
        }
        // Our own vector icons: Media3's built-in ones came out large and blurry on some phones.
        return CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(icon)
            .setDisplayName(getString(label))
            .setSessionCommand(repeatCommand)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_REPEAT = "io.github.hishamhashmi100.yoink.REPEAT"

        /** Off → repeat all → repeat this track → off, like a music app. */
        fun nextRepeatMode(mode: Int) = when (mode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
