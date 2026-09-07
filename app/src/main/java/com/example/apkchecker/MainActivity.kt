package com.example.apkchecker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT <= 32) permission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        setContent { App() }
    }
}

class MainViewModel : ViewModel() {
    var state by mutableStateOf(ScanState())
        private set

    fun scan(context: android.content.Context) {
        if (state.running) return
        viewModelScope.launch {
            state = state.copy(running = true, results = emptyList(), matches = 0)
            try {
                val results = ApkScanner(context).scan { path, count ->
                    state = state.copy(currentPath = path, checkedFiles = count)
                }
                state = state.copy(
                    running = false,
                    results = results,
                    matches = results.size,
                    currentPath = "Сканирование завершено",
                    message = "Полное сканирование всей файловой системы невозможно без специальных привилегий/root. Выполнено максимально глубокое сканирование доступных областей устройства."
                )
            } catch (e: Exception) {
                state = state.copy(running = false, message = "Ошибка: ${e.message}")
            }
        }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("APK Checker") }) }
        ) { padding ->
            Column(
                Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Проверка устройства", style = MaterialTheme.typography.headlineSmall)
                Text("Aris Client • Prax Client • Horizon Modding Kernel • protohack • appolon")

                Button(
                    onClick = { vm.scan(context) },
                    enabled = !vm.state.running,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (vm.state.running) "Сканирование..." else "Начать сканирование")
                }

                if (vm.state.running) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Проверено файлов: ${vm.state.checkedFiles}")
                    Text(vm.state.currentPath, maxLines = 2)
                }

                if (vm.state.message.isNotBlank()) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(vm.state.message, Modifier.padding(12.dp))
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Совпадений: ${vm.state.matches}")
                    Text("Результатов: ${vm.state.results.size}")
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.state.results) { result -> ResultCard(result) }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(r: ScanResult) {
    val (title, icon) = when (r.status) {
        MatchStatus.FOUND -> "🔴 Найдено" to Icons.Default.Warning
        MatchStatus.POSSIBLE -> "🟠 Возможный остаток" to Icons.Default.Info
        MatchStatus.NOT_FOUND -> "🟢 Не найдено" to Icons.Default.CheckCircle
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$title — ${r.target}", style = MaterialTheme.typography.titleMedium)
            Text("Тип: ${r.detectionType}")
            Text("Путь: ${r.path}")
            r.packageName?.let { Text("Package: $it") }
            r.version?.let { Text("Версия: $it") }
            if (r.size > 0) Text("Размер: ${r.size / 1024} KB")
            if (r.sha256 != null) Text("SHA-256: ${r.sha256}")
            Text("Уровень совпадения: ${r.matchLevel}")
            if (r.modified > 0) {
                Text("Дата: ${DateFormat.getDateTimeInstance().format(Date(r.modified))}")
            }
        }
    }
}
