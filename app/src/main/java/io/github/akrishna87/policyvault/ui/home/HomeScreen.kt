package io.github.akrishna87.policyvault.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.akrishna87.policyvault.core.Format
import io.github.akrishna87.policyvault.core.PolicyLogic
import io.github.akrishna87.policyvault.core.PolicyType
import io.github.akrishna87.policyvault.core.Status
import io.github.akrishna87.policyvault.core.Vault
import io.github.akrishna87.policyvault.ui.components.PolicyRow
import io.github.akrishna87.policyvault.ui.components.SectionTitle
import io.github.akrishna87.policyvault.ui.components.StatCard
import io.github.akrishna87.policyvault.ui.components.typeColor
import io.github.akrishna87.policyvault.ui.components.typeIcon

/** The dashboard: totals, what renews soon, and the type mix. */
@Composable
fun HomeScreen(
    vault: Vault,
    today: Long,
    onOpen: (io.github.akrishna87.policyvault.core.Policy) -> Unit,
    onAdd: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val policies = vault.policies
    val active = policies.filter { PolicyLogic.status(it, today) == Status.ACTIVE }
    val renewing = PolicyLogic.renewingSoon(policies, today = today)
    val expired = PolicyLogic.expired(policies, today)

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("PolicyVault") },
            actions = {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Backup and more") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Export backup") }, onClick = { menuOpen = false; onExport() })
                    DropdownMenuItem(text = { Text("Import backup") }, onClick = { menuOpen = false; onImport() })
                }
            },
        )

        if (policies.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            ) {
                Icon(
                    Icons.Filled.Shield, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(72.dp),
                )
                Text(
                    "No policies yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    "Keep every policy — health, motor, term, life, office — in one place, with renewal reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                TextButton(onClick = onAdd) { Text("Add your first policy") }
            }
            return@Column
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatCard("Total cover", Format.short(PolicyLogic.totalCover(policies)), Modifier.weight(1f))
                    StatCard("Premium / year", Format.short(PolicyLogic.totalAnnualPremium(policies)), Modifier.weight(1f))
                }
            }
            item {
                Text(
                    "${active.size} active" + if (expired.isNotEmpty()) " · ${expired.size} expired" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (renewing.isNotEmpty()) {
                item { SectionTitle("Renewing soon") }
                items(renewing, key = { it.id }) { p -> PolicyRow(p, today) { onOpen(p) } }
            }

            if (expired.isNotEmpty()) {
                item { SectionTitle("Expired") }
                items(expired, key = { it.id }) { p -> PolicyRow(p, today) { onOpen(p) } }
            }

            item { SectionTitle("Your mix") }
            val byType = active.groupBy { it.type }.toSortedMap(compareBy { it.label })
            items(byType.entries.toList(), key = { it.key }) { (type, list) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        val color = typeColor(type)
                        Icon(typeIcon(type), null, tint = color)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(type.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${list.size} polic${if (list.size == 1) "y" else "ies"} · ${Format.short(list.sumOf { it.sumInsured ?: 0.0 })} cover",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
