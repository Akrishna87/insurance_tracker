package io.github.akrishna87.policyvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.akrishna87.policyvault.core.Format
import io.github.akrishna87.policyvault.core.Policy
import io.github.akrishna87.policyvault.core.PolicyLogic
import io.github.akrishna87.policyvault.core.PolicyType
import io.github.akrishna87.policyvault.core.Status
import io.github.akrishna87.policyvault.ui.theme.StatusColors

fun typeIcon(type: PolicyType): ImageVector = when (type) {
    PolicyType.HEALTH -> Icons.Filled.HealthAndSafety
    PolicyType.TERM -> Icons.Filled.Security
    PolicyType.LIFE -> Icons.Filled.Favorite
    PolicyType.CAR -> Icons.Filled.DirectionsCar
    PolicyType.BIKE -> Icons.Filled.TwoWheeler
    PolicyType.ACCIDENT -> Icons.Filled.PersonalInjury
    PolicyType.TRAVEL -> Icons.Filled.Flight
    PolicyType.HOME -> Icons.Filled.Home
    PolicyType.OFFICE -> Icons.Filled.BusinessCenter
    PolicyType.OTHER -> Icons.Filled.Shield
}

fun typeColor(type: PolicyType): Color = when (type) {
    PolicyType.HEALTH -> Color(0xFF0D9488)
    PolicyType.TERM -> Color(0xFF4F46E5)
    PolicyType.LIFE -> Color(0xFFDB2777)
    PolicyType.CAR -> Color(0xFF2563EB)
    PolicyType.BIKE -> Color(0xFF7C3AED)
    PolicyType.ACCIDENT -> Color(0xFFEA580C)
    PolicyType.TRAVEL -> Color(0xFF0284C7)
    PolicyType.HOME -> Color(0xFF65A30D)
    PolicyType.OFFICE -> Color(0xFFB45309)
    PolicyType.OTHER -> Color(0xFF64748B)
}

/** A small coloured chip showing how far away the renewal is. */
@Composable
fun DaysChip(policy: Policy, today: Long) {
    val days = PolicyLogic.daysLeft(policy, today)
    val text = when {
        days == null -> "No renewal date"
        days < 0 -> "Expired ${Format.date(policy.renewalDate)}"
        days == 0L -> "Renews today"
        else -> "${days}d left"
    }
    val color = when {
        days == null || days > 30 -> MaterialTheme.colorScheme.onSurfaceVariant
        days > 7 -> StatusColors.warning()
        days >= 0 -> StatusColors.danger()
        else -> StatusColors.danger()
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** One policy in a list: icon, name, subtitle and the renewal chip. */
@Composable
fun PolicyRow(policy: Policy, today: Long, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            val color = typeColor(policy.type)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
            ) {
                Icon(typeIcon(policy.type), null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    policy.insurer + if (policy.planName.isNotBlank()) " · ${policy.planName}" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = buildList {
                    add(policy.type.label)
                    if (policy.policyNumber.isNotBlank()) add(policy.policyNumber)
                    policy.sumInsured?.let { add("Cover ${Format.short(it)}") }
                    if (policy.members.isNotEmpty()) add("${policy.members.size} member${if (policy.members.size == 1) "" else "s"}")
                }.joinToString(" · ")
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (policy.renewalDate != null) {
                    Text(
                        "Renewal ${Format.date(policy.renewalDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (PolicyLogic.status(policy, today) == Status.EXPIRED) StatusColors.danger()
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DaysChip(policy, today)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** A card showing one number, e.g. total cover. */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A read-only row on the detail screen: label left, value right. */
@Composable
fun DetailRow(label: String, value: String) {
    if (value.isBlank() || value == "—") return
    Row(Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

private const val DAY_MS = 86_400_000L

/** A button that shows a date and opens the Material date picker. */
@Composable
fun DateField(label: String, epochDay: Long?, onPicked: (Long?) -> Unit, allowClear: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Format.date(epochDay),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (allowClear && epochDay != null) {
                TextButton(onClick = { onPicked(null) }) { Text("Clear") }
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay?.let { it * DAY_MS })
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onPicked(state.selectedDateMillis?.let { it / DAY_MS })
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}
