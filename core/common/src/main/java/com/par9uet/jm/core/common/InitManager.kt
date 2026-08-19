package com.par9uet.jm.core.common

import kotlinx.coroutines.CompletableDeferred

class InitManager {
    val deferred = CompletableDeferred<String>()
}