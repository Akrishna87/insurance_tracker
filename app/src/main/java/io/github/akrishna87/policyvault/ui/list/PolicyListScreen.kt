package io.github.akrishna87.policyvault.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.akrishna87.policyvault.core.Policy
import io.github.akrishna87.policyvault.core.PolicyLogic
import io.github.akrishna87.policyvault.core.Status
import io.github.akrishna87.policyvault.core.Vault
import io.github.akrishna87.policyvault.ui.components.PolicyRow

/** Every policy, searchable and filterable by type. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyListScreen(
    vault: Vault,
    today: Long,
    onOpen: (Policy) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<io.github.akrishna87.policyvault.core.PolicyType?>(null) }
    val policies = vault.policies

    val visible = remember(policies, query, filter, today) {
        val q = query.trim().lowercase()
        policies.filter { p ->
            (filter == null || p.type == filter) &&
                (q.isEmpty() || p.insurer.lowercase().contains(q) ||
                    p.planName.lowercase().contains(q) ||
                    p.policyNumber.lowercase().contains(q) ||
                    p.members.any { it.lowercase().contains(q) } ||
                    p.notes.lowercase().contains(q))
        }.sortedWith(
            // Active first by soonest renewal, then expired, then policies without a date.
            compareBy<Policy> { PolicyLogic.status(it, today) == Status.EXPIRED }
                .thenBy { it.renewalDate == null }
                .thenBy { it.renewalDate ?: Long.MAX_VALUE },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Surface(shadowElevation = 2.dp) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search insurer, policy no., member…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                    }
                    items(io.github.akrishna87.policyvault.core.PolicyType.entries) { type ->
                        FilterChip(
                            selected = filter == type,
                            onClick = { filter = if (filter == type) null else type },
                            label = { Text(type.label) },
                        )
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (visible.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (policies.isEmpty()) "No policies yet. Add one from the Home tab."
                            else "Nothing matches that search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(visible, key = { it.id }) { p -> PolicyRow(p, today) { onOpen(p) } }
            }
        }
    }
}
