package io.github.hishamhashmi100.yoink.ui.theme

import android.app.Activity
import android.os.Build
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.yoink

/**
 * Over "Your wallpaper" every screen is see-through, so the usual slide shows two screens on top of each other on
 * the way in and out. There, the new screen fades in over the wallpaper and the one it covers steps aside at once.
 */
object WallpaperTransitions {

    /** Call from onCreate: how this screen opens. */
    fun opening(activity: Activity) {
        if (!overWallpaper(activity)) return
        if (Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.yoink_fade_in, 0)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.yoink_fade_in, 0)
        }
    }

    /**
     * Wraps finish(): how this screen closes. Checked now rather than at opening, since the look can change while
     * the screen is open (on the Appearance screen, say).
     */
    fun finishing(activity: Activity, finish: () -> Unit) {
        val fade = overWallpaper(activity)
        if (Build.VERSION.SDK_INT >= 34) {
            if (fade) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.yoink_fade_in, 0)
            } else {
                activity.clearOverrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE)
            }
            finish()
        } else {
            finish()
            @Suppress("DEPRECATION")
            if (fade) activity.overridePendingTransition(R.anim.yoink_fade_in, 0)
        }
    }

    private fun overWallpaper(activity: Activity) = activity.yoink.settings.current().appearance.showsWallpaper
}
