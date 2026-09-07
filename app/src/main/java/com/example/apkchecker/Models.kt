package com.example.apkchecker

enum class MatchStatus { FOUND, POSSIBLE, NOT_FOUND }

data class ScanResult(
    val target: String,
    val status: MatchStatus,
    val detectionType: String,
    val path: String,
    val packageName: String? = null,
    val version: String? = null,
    val size: Long = 0L,
    val modified: Long = 0L,
    val sha256: String? = null,
    val matchLevel: String = ""
)

data class ScanState(
    val running: Boolean = false,
    val currentPath: String = "",
    val checkedFiles: Long = 0,
    val matches: Int = 0,
    val results: List<ScanResult> = emptyList(),
    val message: String = ""
)
