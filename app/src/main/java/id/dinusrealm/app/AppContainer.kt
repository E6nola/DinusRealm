package id.dinusrealm.app

import android.content.Context
import id.dinusrealm.app.core.crypto.KeystoreVault
import id.dinusrealm.app.core.db.DinusDbHelper
import id.dinusrealm.app.core.network.DinusHttpClient
import id.dinusrealm.app.core.network.SiadinApi
import id.dinusrealm.app.core.network.UrlConnectionTransport
import id.dinusrealm.app.core.repo.DinusRepository

/**
 * Manual, zero-reflection dependency container (replaces Hilt/Dagger for minimal footprint).
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val vault: KeystoreVault by lazy { KeystoreVault(appContext) }
    val db: DinusDbHelper by lazy { DinusDbHelper(appContext) }
    val transport: UrlConnectionTransport by lazy { UrlConnectionTransport() }

    val httpClient: DinusHttpClient by lazy {
        DinusHttpClient(
            transport = transport,
            authStorage = vault,
            onSessionInvalidated = { repo.onSessionInvalidated() }
        )
    }

    val api: SiadinApi by lazy { SiadinApi(httpClient) }

    val repo: DinusRepository by lazy {
        DinusRepository(
            appContext = appContext,
            vault = vault,
            db = db,
            api = api
        )
    }
}
