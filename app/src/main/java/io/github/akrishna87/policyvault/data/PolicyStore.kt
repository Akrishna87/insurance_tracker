package io.github.akrishna87.policyvault.data

import android.content.Context
import io.github.akrishna87.policyvault.core.Policy
import io.github.akrishna87.policyvault.core.PolicyIO
import io.github.akrishna87.policyvault.core.Vault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

/**
 * The user's policies, kept in a single JSON file in the app's private storage.
 * Nothing leaves the phone; export/import for backups goes through JSON copies.
 */
class PolicyStore(context: Context) {
    private val file: File = context.filesDir.resolve(FILE)

    private val mutex = Mutex()
    private val _vault = MutableStateFlow(load())
    val vault: StateFlow<Vault> = _vault.asStateFlow()

    private fun load(): Vault = try {
        if (file.exists()) PolicyIO.decode(file.readText()) else Vault()
    } catch (_: Exception) {
        Vault() // A corrupt file is worse than a fresh start; the old file is left alone.
    }

    private suspend fun persist(vault: Vault) = mutex.withLock {
        _vault.value = vault
        val tmp = File(file.parentFile, FILE + ".tmp")
        tmp.writeText(PolicyIO.encode(vault))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    /** Adds a policy, or saves changes to an existing one. */
    suspend fun save(policy: Policy) = persist(
        _vault.value.let { v ->
            val stamped = policy.copy(updatedAt = System.currentTimeMillis())
            val existing = v.policies.indexOfFirst { it.id == policy.id }
            val policies = if (existing >= 0) v.policies.toMutableList().also { it[existing] = stamped }
            else v.policies + stamped.copy(createdAt = System.currentTimeMillis())
            // Dates may have changed: allow today's reminders for this policy again.
            v.copy(policies = policies, notified = v.notified - policy.id)
        },
    )

    suspend fun delete(id: String) = persist(
        _vault.value.copy(policies = _vault.value.policies.filterNot { it.id == id }, notified = _vault.value.notified - id),
    )

    /** Shifts a policy one year forward after renewal. */
    suspend fun markRenewed(id: String) {
        val p = _vault.value.policies.firstOrNull { it.id == id } ?: return
        save(io.github.akrishna87.policyvault.core.PolicyLogic.renewed(p))
    }

    /** Records that a reminder notification was sent for a policy today. */
    suspend fun markNotified(id: String, day: Long) = persist(
        _vault.value.copy(notified = _vault.value.notified + (id to day)),
    )

    /**
     * Merges a backup into the vault: policies whose id already exists are skipped.
     * Returns the number of policies imported.
     */
    suspend fun import(text: String): Int {
        val incoming = PolicyIO.decode(text).policies
        val existing = _vault.value.policies.map { it.id }.toSet()
        val fresh = incoming.filterNot { it.id in existing }
        if (fresh.isNotEmpty()) persist(_vault.value.copy(policies = _vault.value.policies + fresh))
        return fresh.size
    }

    /** The vault as JSON, for backups. */
    fun export(): String = PolicyIO.encode(_vault.value)

    /** A new id for an added policy. */
    fun newId(): String = UUID.randomUUID().toString()

    companion object {
        private const val FILE = "vault.json"
    }
}
