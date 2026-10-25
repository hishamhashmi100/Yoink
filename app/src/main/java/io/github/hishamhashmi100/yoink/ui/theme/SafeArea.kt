package io.github.hishamhashmi100.yoink.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/**
 * Where content is safe on any phone: clear of the status bar, camera cutouts, curved screen edges and the
 * keyboard. (The system's defaults only keep clear of the bars, so on curved or notched phones content could
 * run into the edges.)
 */
object SafeArea {

    /** For a screen's content (a Scaffold's contentWindowInsets). */
    val content: WindowInsets
        @Composable get() = WindowInsets.safeDrawing

    /** For a top app bar: the top and the sides. */
    val topBar: WindowInsets
        @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

    /**
     * For something at the bottom of the screen, like the mini player: the sides, plus the bottom edge, which
     * with the navigation buttons hidden is still where the swipe-up gesture (and rounded screen corners) sit.
     */
    val bottomBar: WindowInsets
        @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .union(WindowInsets.mandatorySystemGestures.only(WindowInsetsSides.Bottom))
}
