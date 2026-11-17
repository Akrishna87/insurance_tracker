package io.github.akrishna87.policyvault.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.akrishna87.policyvault.PolicyVaultApp
import io.github.akrishna87.policyvault.core.PolicyLogic
import io.github.akrishna87.policyvault.core.PolicyType
import io.github.akrishna87.policyvault.ui.detail.PolicyDetailScreen
import io.github.akrishna87.policyvault.ui.edit.EditPolicyScreen
import io.github.akrishna87.policyvault.ui.home.HomeScreen
import io.github.akrishna87.policyvault.ui.list.PolicyListScreen
import io.github.akrishna87.policyvault.ui.theme.PolicyVaultTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the user is: one of the two tabs, or a screen over them. */
sealed interface Dest {
    data object Home : Dest
    data object List : Dest
    data class Detail(val id: String) : Dest
    data class Edit(val id: String?) : Dest
}

@Composable
fun VaultRoot() {
    PolicyVaultTheme {
        val context = LocalContext.current
        val app = context.applicationContext as PolicyVaultApp
        val scope = rememberCoroutineScope()
        val vault by app.store.vault.collectAsState()
        val today = remember { PolicyLogic.today() }

        var tab by remember { mutableIntStateOf(0) }
        var overlay by remember { mutableStateOf<Dest?>(null) }

        // Ask for notification permission once, for the renewal reminders.
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Back from an overlay screen returns to the tabs.
        BackHandler(enabled = overlay != null) { overlay = null }

        // Backup export: the whole vault as JSON.
        val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) scope.launch { writeBackup(context, uri, app.store.export()) }
        }
        // Backup import: merged in, skipping policies already present.
        val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) scope.launch {
                val text = readBackup(context, uri)
                if (text != null) app.store.import(text)
            }
        }

        Scaffold(
            bottomBar = {
                if (overlay == null) NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Filled.Home, null) },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Filled.List, null) },
                        label = { Text("Policies") },
                    )
                }
            },
            floatingActionButton = {
                if (overlay == null) ExtendedFloatingActionButton(
                    onClick = { overlay = Dest.Edit(null) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Add policy") },
                )
            },
        ) { padding ->
            Box(Modifier
                .fillMaxSize()
                .padding(padding)) {
                when (val d = overlay) {
                    is Dest.Edit -> EditPolicyScreen(
                        initial = d.id?.let { id -> vault.policies.firstOrNull { it.id == id } },
                        onDone = { overlay = null },
                    )
                    is Dest.Detail -> PolicyDetailScreen(
                        policy = vault.policies.firstOrNull { it.id == d.id },
                        onEdit = { overlay = Dest.Edit(d.id) },
                        onClose = { overlay = null },
                    )
                    null -> if (tab == 0) {
                        HomeScreen(
                            vault = vault,
                            today = today,
                            onOpen = { overlay = Dest.Detail(it.id) },
                            onAdd = { overlay = Dest.Edit(null) },
                            onExport = { export.launch("policyvault-backup.json") },
                            onImport = { import.launch(arrayOf("application/json")) },
                        )
                    } else {
                        PolicyListScreen(
                            vault = vault,
                            today = today,
                            onOpen = { overlay = Dest.Detail(it.id) },
                        )
                    }
                    // Home and List are only ever used as tabs, never overlays.
                    else -> {}
                }
            }
        }
    }
}

private suspend fun writeBackup(context: android.content.Context, uri: Uri, text: String) =
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
    }

private suspend fun readBackup(context: android.content.Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } catch (_: Exception) {
            null
        }
    }
