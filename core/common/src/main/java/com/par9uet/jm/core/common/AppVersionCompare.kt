package com.par9uet.jm.core.common

fun normalizeVersion(value: String): String = value.trim()
    .removePrefix("v")
    .removePrefix("V")
    .substringBefore(" ")
    .substringBefore("-")

fun compareVersion(left: String, right: String): Int {
    val leftParts = normalizeVersion(left).split(".").map { it.toIntOrNull() ?: 0 }
    val rightParts = normalizeVersion(right).split(".").map { it.toIntOrNull() ?: 0 }
    val count = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until count) {
        val leftPart = leftParts.getOrElse(index) { 0 }
        val rightPart = rightParts.getOrElse(index) { 0 }
        if (leftPart != rightPart) return leftPart.compareTo(rightPart)
    }
    return 0
}
