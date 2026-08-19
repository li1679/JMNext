package com.par9uet.jm.core.common

import java.io.File

fun tryCreateDir(dir: File): File {
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}