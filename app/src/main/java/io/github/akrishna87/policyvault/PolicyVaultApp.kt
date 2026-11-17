package io.github.akrishna87.policyvault

import android.app.Application
import io.github.akrishna87.policyvault.data.PolicyStore
import io.github.akrishna87.policyvault.work.Notifier
import io.github.akrishna87.policyvault.work.ReminderWorker

class PolicyVaultApp : Application() {
    lateinit var store: PolicyStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = PolicyStore(this)
        Notifier.createChannels(this)
        ReminderWorker.schedule(this)
    }

    companion object {
        lateinit var instance: PolicyVaultApp
            private set
    }
}
