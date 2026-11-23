package id.dinusrealm.app

import android.app.Application
import id.dinusrealm.app.core.engine.ClassScheduler
import id.dinusrealm.app.core.engine.NotificationHelper

class DinusRealmApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannels(this)
        if (container.vault.hasCredentials()) {
            ClassScheduler.schedulePeriodicJobs(this)
        }
    }
}
