package com.example.apkchecker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intentAll = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intentAll)
            } catch (_: Exception) {
                permission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("APK Checker Запущен!", style = MaterialTheme.typography.headlineMedium)
                        Text("Разрешения на доступ к файлам успешно запрошены.")
                        Button(onClick = { finish() }) {
                            Text("Закрыть приложение")
                        }
                    }
                }
            }
        }
    }
}

// Заглушки, чтобы сборщик не ругался на отсутствие классов в других файлах
class ScanState(val running: Boolean = false, val results: List<ScanResult> = emptyList(), val matches: Int = 0, val currentPath: String = "", val checkedFiles: Int = 0, val message: String = "")
class ScanResult(val status: MatchStatus = MatchStatus.NOT_FOUND, val target: String = "", val detectionType: String = "", val path: String = "", val packageName: String? = null, val version: String? = null, val size: Long = 0, val sha256: String? = null, val matchLevel: String = "", val modified: Long = 0)
enum class MatchStatus { FOUND, POSSIBLE, NOT_FOUND }
