package ch.rolf.androidweather.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Smallest width, so phones stay phone-layout even in landscape. */
@Composable
fun isTablet(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600

@Composable
fun isTabletLandscape(): Boolean {
    val config = LocalConfiguration.current
    return config.smallestScreenWidthDp >= 600 && config.screenWidthDp >= 900
}

@Composable
fun TabletWidth(
    max: Dp = 880.dp,
    content: @Composable () -> Unit
) {
    if (!isTablet()) {
        content()
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = max).fillMaxWidth()) {
            content()
        }
    }
}
