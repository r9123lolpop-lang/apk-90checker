package com.example.apkchecker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class ApkScanner(private val context: Context) {
    private val targets = listOf(
        "Aris Client" to listOf("aris", "arisclient"),
        "Prax Client" to listOf("prax", "praxclient"),
        "Horizon Modding Kernel" to listOf("horizon", "modding", "kernel"),
        "protohack" to listOf("protohack"),
        "appolon" to listOf("appolon")
    )

    suspend fun scan(onProgress: (String, Long) -> Unit): List<ScanResult> =
        withContext(Dispatchers.IO) {
            val found = mutableListOf<ScanResult>()
            var checked = 0L

            fun inspect(file: File) {
                coroutineContext.ensureActive()
                checked++
                if (checked % 25L == 0L) onProgress(file.path, checked)

                val lower = file.name.lowercase()
                targets.forEach { (target, keys) ->
                    val hit = keys.any { lower.contains(it) }
                    if (hit) {
                        val apk = lower.endsWith(".apk") || lower.endsWith(".apks")
                        found += ScanResult(
                            target = target,
                            status = if (apk) MatchStatus.FOUND else MatchStatus.POSSIBLE,
                            detectionType = if (apk) "APK-файл" else "Файл/каталог",
                            path = file.absolutePath,
                            size = if (file.isFile) file.length() else 0L,
                            modified = file.lastModified(),
                            sha256 = if (apk && file.isFile) sha256(file) else null,
                            matchLevel = if (apk) "Имя файла" else "Ключевое слово"
                        )
                    }
                }
            }

            val roots = linkedSetOf<File>()
            Environment.getExternalStorageDirectory()?.let { roots += it }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { roots += it }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let { roots += it }
            File(Environment.getExternalStorageDirectory(), "Android/media").let { roots += it }
            File(Environment.getExternalStorageDirectory(), "Android/data").let { roots += it }
            File(Environment.getExternalStorageDirectory(), "Android/obb").let { roots += it }

            fun walk(dir: File, depth: Int = 0) {
                coroutineContext.ensureActive()
                if (!dir.exists() || !dir.canRead() || depth > 30) return
                if (dir.isFile) { inspect(dir); return }
                val children = try { dir.listFiles() } catch (_: SecurityException) { null } ?: return
                children.forEach { walk(it, depth + 1) }
            }
            roots.forEach { walk(it) }

            // Installed applications
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            apps.forEach { app ->
                coroutineContext.ensureActive()
                val label = pm.getApplicationLabel(app).toString()
                val haystack = "${label} ${app.packageName} ${app.sourceDir}".lowercase()
                targets.forEach { (target, keys) ->
                    if (keys.any { haystack.contains(it) }) {
                        val info = try { pm.getPackageInfo(app.packageName, 0) } catch (_: Exception) { null }
                        found += ScanResult(
                            target, MatchStatus.FOUND, "Установленное приложение",
                            app.sourceDir ?: "", app.packageName,
                            info?.versionName ?: "unknown",
                            File(app.sourceDir ?: "").length(),
                            File(app.sourceDir ?: "").lastModified(),
                            null, "Label/package/sourceDir"
                        )
                    }
                }
            }

            onProgress("Сканирование завершено", checked)
            found
        }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
