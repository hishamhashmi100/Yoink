package io.github.hishamhashmi100.yoink.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse

/**
 * A label that stays on one line, shrinking a little when it doesn't fit: a narrow screen, a large font size
 * in the phone's settings, or a wide font (like Pixel). For buttons and tabs, which shouldn't grow a second line.
 */
@Composable
fun OneLineText(text: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    val size = style.fontSize.takeOrElse { 14.sp }
    BasicText(
        text,
        modifier = modifier,
        style = style.copy(color = style.color.takeOrElse { LocalContentColor.current }),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = size, stepSize = 0.5.sp),
    )
}
