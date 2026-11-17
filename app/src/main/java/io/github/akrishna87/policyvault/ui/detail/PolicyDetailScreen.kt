package io.github.akrishna87.policyvault.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.akrishna87.policyvault.PolicyVaultApp
import io.github.akrishna87.policyvault.core.Format
import io.github.akrishna87.policyvault.core.Policy
import io.github.akrishna87.policyvault.core.PolicyLogic
import io.github.akrishna87.policyvault.core.Status
import io.github.akrishna87.policyvault.ui.components.DaysChip
import io.github.akrishna87.policyvault.ui.components.DetailRow
import io.github.akrishna87.policyvault.ui.components.SectionTitle
import io.github.akrishna87.policyvault.ui.components.StatCard
import io.github.akrishna87.policyvault.ui.components.typeColor
import io.github.akrishna87.policyvault.ui.components.typeIcon
import io.github.akrishna87.policyvault.ui.theme.StatusColors
import kotlinx.coroutines.launch

/** Everything about one policy, with edit, renew and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyDetailScreen(
    policy: Policy?,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as PolicyVaultApp)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Policy") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = onEdit) { Text("Edit") }
                },
            )
        },
    ) { padding ->
        if (policy == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This policy is no longer in the vault.")
            }
            return@Scaffold
        }
        val today = remember { PolicyLogic.today() }
        val expired = PolicyLogic.status(policy, today) == Status.EXPIRED

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Header: icon, insurer, plan, status.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    val color = typeColor(policy.type)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .background(color.copy(alpha = 0.15f), CircleShape),
                    ) {
                        Icon(typeIcon(policy.type), null, tint = color, modifier = Modifier.size(26.dp))
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    ) {
                        Text(policy.insurer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        if (policy.planName.isNotBlank()) {
                            Text(
                                policy.planName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            policy.type.label + if (policy.policyNumber.isNotBlank()) " · ${policy.policyNumber}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DaysChip(policy, today)
                }
            }

            // The numbers.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Sum insured", Format.inr(policy.sumInsured), Modifier.weight(1f))
                if (policy.premium != null) {
                    StatCard("Premium", Format.inr(policy.premium), Modifier.weight(1f))
                }
                PolicyLogic.annualPremium(policy)?.let {
                    StatCard("Per year", Format.inr(it), Modifier.weight(1f))
                }
            }

            SectionTitle("Details")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    DetailRow("Cover starts", Format.date(policy.startDate))
                    DetailRow("Renews / expires", Format.date(policy.renewalDate))
                    if (policy.vehicleNumber.isNotBlank()) DetailRow("Vehicle", policy.vehicleNumber)
                    if (policy.employer.isNotBlank()) DetailRow("Employer / group", policy.employer)
                    if (policy.members.isNotEmpty()) DetailRow("Members covered", policy.members.joinToString(", "))
                    if (policy.nominee.isNotBlank()) DetailRow("Nominee", policy.nominee)
                    if (policy.claimContact.isNotBlank()) DetailRow("TPA / claims", policy.claimContact)
                    DetailRow("Reminders", if (policy.reminderDays <= 0) "Off" else "${policy.reminderDays}, 7 and 1 days before")
                    if (policy.notes.isNotBlank()) DetailRow("Notes", policy.notes)
                }
            }

            if (policy.renewalDate != null && !expired) {
                OutlinedButton(
                    onClick = { scope.launch { app.store.markRenewed(policy.id) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Autorenew, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark renewed (shifts dates a year forward)")
                }
            }

            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete policy", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this policy?") },
            text = { Text("${policy?.insurer ?: "This policy"} will be removed from the vault. A backup taken earlier still has it.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val id = policy?.id ?: return@TextButton
                    scope.launch {
                        app.store.delete(id)
                        onClose()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}
