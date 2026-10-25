package io.github.hishamhashmi100.yoink.ui.theme

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.ui.player.PlayerActivity
import io.github.hishamhashmi100.yoink.ui.settings.AppearanceActivity
import io.github.hishamhashmi100.yoink.ui.settings.SettingsActivity
import io.github.hishamhashmi100.yoink.yoink
import java.util.WeakHashMap

/**
 * Yoink's full screens over "Your wallpaper".
 *
 * Every screen is see-through there, so while one screen slides in over another (or back out) both would show at
 * once. A screen's content disappears while another of Yoink's screens covers it, or while it closes, and comes
 * back when it's on top again, the way the mini player gives way to the full-screen player. [YoinkTheme] does
 * the fading.
 */
object WallpaperScreens {

    // Full screens that cover the one they're opened from. (Not the share sheet, which floats over it.)
    private val fullScreens = setOf(
        SettingsActivity::class.java.name,
        AppearanceActivity::class.java.name,
        PlayerActivity::class.java.name,
    )

    private val covered = WeakHashMap<Activity, MutableState<Boolean>>()

    /** True while another screen covers [activity], or while it closes. Back to false when it's on top again. */
    fun covered(activity: Activity): MutableState<Boolean> = covered.getOrPut(activity) { mutableStateOf(false) }

    /** Call before super.onCreate: the window starts out showing the wallpaper, rather than switching later. */
    fun applyTheme(activity: Activity) {
        if (activity.yoink.settings.current().appearance.showsWallpaper) activity.setTheme(R.style.Theme_Yoink_Wallpaper)
    }

    /** Call from startActivity. */
    fun starting(activity: Activity, intent: Intent) {
        if (intent.component?.className in fullScreens) covered(activity).value = true
    }

    /** Call from finish. */
    fun finishing(activity: Activity) {
        covered(activity).value = true
    }
}
