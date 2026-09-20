package com.par9uet.jm.ui.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow

val LocalTabScrollEvents = staticCompositionLocalOf<MutableSharedFlow<String>?> { null }

@Composable
fun OnTabScrollToTop(route: String, enabled: Boolean = true, scroll: suspend () -> Unit) {
    val events = LocalTabScrollEvents.current
    val currentScroll = rememberUpdatedState(scroll)
    LaunchedEffect(events, route, enabled) {
        if (enabled) events?.collect { if (it == route) currentScroll.value() }
    }
}
