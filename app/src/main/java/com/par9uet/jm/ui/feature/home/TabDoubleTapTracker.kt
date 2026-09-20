package com.par9uet.jm.ui.feature.home

internal class TabDoubleTapTracker(private val timeoutMillis: Long) {
    private var previousRoute: String? = null
    private var previousTime = 0L

    fun click(route: String, timeMillis: Long): Boolean {
        val doubleTap = route != "user" && previousRoute == route &&
            timeMillis - previousTime in 0..timeoutMillis
        previousRoute = if (doubleTap) null else route
        previousTime = timeMillis
        return doubleTap
    }
}
