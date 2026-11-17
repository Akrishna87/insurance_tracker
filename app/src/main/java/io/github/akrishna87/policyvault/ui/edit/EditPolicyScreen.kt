package io.github.akrishna87.policyvault.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.akrishna87.policyvault.PolicyVaultApp
import io.github.akrishna87.policyvault.core.Frequency
import io.github.akrishna87.policyvault.core.Policy
import io.github.akrishna87.policyvault.core.PolicyLogic
import io.github.akrishna87.policyvault.core.PolicyType
import io.github.akrishna87.policyvault.ui.components.DateField
import io.github.akrishna87.policyvault.ui.components.SectionTitle
import io.github.akrishna87.policyvault.ui.components.typeIcon
import kotlinx.coroutines.launch

private const val DAY_MS = 86_400_000L

/** Add a policy or edit an existing one. Everything except type and insurer is optional. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditPolicyScreen(
    initial: Policy?,
    onDone: () -> Unit,
) {
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as PolicyVaultApp)
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(initial?.type ?: PolicyType.HEALTH) }
    var insurer by remember { mutableStateOf(initial?.insurer ?: "") }
    var insurerError by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf(initial?.planName ?: "") }
    var number by remember { mutableStateOf(initial?.policyNumber ?: "") }
    var cover by remember { mutableStateOf(initial?.sumInsured?.let { formatAmount(it) } ?: "") }
    var premium by remember { mutableStateOf(initial?.premium?.let { formatAmount(it) } ?: "") }
    var frequency by remember { mutableStateOf(initial?.frequency) }
    var start by remember { mutableStateOf(initial?.startDate) }
    var renewal by remember { mutableStateOf(initial?.renewalDate) }
    val members = remember { mutableStateListOf<String>().apply { addAll(initial?.members ?: emptyList()) } }
    var memberInput by remember { mutableStateOf("") }
    var nominee by remember { mutableStateOf(initial?.nominee ?: "") }
    var vehicle by remember { mutableStateOf(initial?.vehicleNumber ?: "") }
    var employer by remember { mutableStateOf(initial?.employer ?: "") }
    var claimContact by remember { mutableStateOf(initial?.claimContact ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var reminderDays by remember { mutableStateOf(initial?.reminderDays ?: Policy.DEFAULT_REMINDER_DAYS) }

    val today = remember { PolicyLogic.today() }

    fun save() {
        if (insurer.isBlank()) {
            insurerError = true
            return
        }
        val policy = Policy(
            id = initial?.id ?: app.store.newId(),
            type = type,
            insurer = insurer.trim(),
            planName = plan.trim(),
            policyNumber = number.trim(),
            sumInsured = cover.toDoubleOrNull(),
            premium = premium.toDoubleOrNull(),
            frequency = frequency,
            startDate = start,
            renewalDate = renewal,
            members = members.map { it.trim() }.filter { it.isNotEmpty() },
            nominee = nominee.trim(),
            vehicleNumber = vehicle.trim(),
            employer = employer.trim(),
            claimContact = claimContact.trim(),
            notes = notes.trim(),
            reminderDays = reminderDays,
            createdAt = initial?.createdAt ?: 0L,
            updatedAt = 0L,
        )
        scope.launch {
            app.store.save(policy)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Add policy" else "Edit policy") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { save() }) { Text("Save") }
                    if (initial != null) {
                        TextButton(onClick = {
                            scope.launch {
                                app.store.delete(initial.id)
                                onDone()
                            }
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle("What kind of policy?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PolicyType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label) },
                        leadingIcon = if (type == t) {
                            { Icon(typeIcon(t), null, Modifier.size(18.dp)) }
                        } else null,
                    )
                }
            }

            OutlinedTextField(
                value = insurer,
                onValueChange = { insurer = it; insurerError = false },
                label = { Text("Insurer *") },
                placeholder = { Text("e.g. Star Health, LIC, HDFC Ergo") },
                singleLine = true,
                isError = insurerError,
                supportingText = if (insurerError) {
                    { Text("The insurer's name is needed") }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = plan,
                onValueChange = { plan = it },
                label = { Text("Plan name") },
                placeholder = { Text("e.g. Family Floater Optima") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Policy number") },
                placeholder = { Text("From the policy document") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            if (type.isMotor) {
                OutlinedTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = { Text("Vehicle number") },
                    placeholder = { Text("e.g. TN 01 AB 1234") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (type == PolicyType.OFFICE) {
                OutlinedTextField(
                    value = employer,
                    onValueChange = { employer = it },
                    label = { Text("Employer / group") },
                    placeholder = { Text("e.g. Acme Corp") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = cover,
                onValueChange = { cover = it },
                label = { Text("Sum insured / cover (₹)") },
                placeholder = { Text("e.g. 500000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Premium")
            OutlinedTextField(
                value = premium,
                onValueChange = { premium = it },
                label = { Text("Premium (₹, per payment)") },
                placeholder = { Text("Leave blank if the employer pays") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Frequency.entries.forEach { f ->
                    FilterChip(
                        selected = frequency == f,
                        onClick = { frequency = if (frequency == f) null else f },
                        label = { Text(f.label) },
                    )
                }
            }

            SectionTitle("Dates")
            DateField("Cover starts", start, onPicked = { start = it }, allowClear = false)
            DateField("Renews / expires on", renewal, onPicked = { renewal = it })
            if (renewal != null && start != null && renewal!! < start!!) {
                Text(
                    "The renewal date is before the start date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SectionTitle("Members covered")
            if (members.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    members.forEach { m ->
                        FilterChip(
                            selected = true,
                            onClick = { members.remove(m) },
                            label = { Text(m) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = memberInput,
                    onValueChange = { memberInput = it },
                    label = { Text("Add a member") },
                    placeholder = { Text("e.g. Self, Spouse, Father") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        if (memberInput.isNotBlank() && members.none { it.equals(memberInput.trim(), ignoreCase = true) }) {
                            members.add(memberInput.trim())
                        }
                        memberInput = ""
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Add") }
            }

            OutlinedTextField(
                value = nominee,
                onValueChange = { nominee = it },
                label = { Text("Nominee") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            if (type == PolicyType.HEALTH || type == PolicyType.OFFICE || type == PolicyType.ACCIDENT) {
                OutlinedTextField(
                    value = claimContact,
                    onValueChange = { claimContact = it },
                    label = { Text("TPA / claim contact") },
                    placeholder = { Text("e.g. MediAssist, 1800-xxx") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionTitle("Reminder")
            Text(
                "A notification before the renewal date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 15, 7).forEach { d ->
                    FilterChip(
                        selected = reminderDays == d,
                        onClick = { reminderDays = if (reminderDays == d) 0 else d },
                        label = { Text("$d days before") },
                    )
                }
                FilterChip(
                    selected = reminderDays == 0,
                    onClick = { reminderDays = if (reminderDays == 0) Policy.DEFAULT_REMINDER_DAYS else 0 },
                    label = { Text("Off") },
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (initial == null) "Save policy" else "Save changes")
            }
        }
    }
}

/** 125000 -> "125000" without trailing ".0". */
private fun formatAmount(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
