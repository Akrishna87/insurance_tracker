package io.github.akrishna87.policyvault.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.akrishna87.policyvault.PolicyVaultApp
import io.github.akrishna87.policyvault.core.Format
import io.github.akrishna87.policyvault.core.Policy
import io.github.akrishna87.policyvault.core.PolicyLogic
import java.util.concurrent.TimeUnit

/**
 * Checks every policy once a day and sends the renewal reminders:
 * the policy's own lead time (30 days by default), then 7 days, 1 day and the day itself.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PolicyVaultApp
        val today = PolicyLogic.today()
        var vault = app.store.vault.value
        var due = vault.policies.filter { PolicyLogic.reminderDue(it, vault, today) }
        // The worker may run several times a day (periodic + the check on open).
        while (due.isNotEmpty()) {
            for (policy in due) {
                notify(applicationContext, policy, today)
                app.store.markNotified(policy.id, today)
            }
            vault = app.store.vault.value
            due = vault.policies.filter { PolicyLogic.reminderDue(it, vault, today) }
        }
        return Result.success()
    }

    private fun notify(context: Context, policy: Policy, today: Long) {
        val days = PolicyLogic.daysLeft(policy, today) ?: return
        val whenText = Format.date(policy.renewalDate)
        val title: String
        val text: String
        if (days == 0L) {
            title = "${policy.insurer} renews today"
            text = "Your ${policy.type.label.lowercase()} policy is up for renewal today ($whenText)."
        } else {
            title = "${policy.insurer} renews in $days day${if (days == 1L) "" else "s"}"
            text = "Your ${policy.type.label.lowercase()} policy comes up for renewal on $whenText."
        }
        Notifier.showRenewal(context, "renewal-${policy.id}", title, text)
    }

    companion object {
        private const val NAME = "renewal-reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            // Also check as soon as the app is opened, in case a threshold day was missed.
            val now = OneTimeWorkRequestBuilder<ReminderWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(NAME + "-now", ExistingWorkPolicy.KEEP, now)
        }
    }
}
