package manual.app.premium

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class BillingClientManager(
    application: Application,
    private val retryConnectionDelay: Long
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val statusStateFlow = MutableStateFlow<Status?>(null)
    private val billingResultStateFlow =
        MutableStateFlow<Pair<BillingResult, List<Purchase>?>?>(null)
    private val billingClient = BillingClient.newBuilder(application)
        .setListener { billingResult, purchases ->
            billingResultStateFlow.value = billingResult to purchases
        }
        .enablePendingPurchases()
        .build()

    init {
        connectBillingClient()
    }

    fun statusFlow(): Flow<Status?> = statusStateFlow

    fun purchaseResultFlow(): Flow<Pair<BillingResult, List<Purchase>?>?> = billingResultStateFlow

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    statusStateFlow.value = Status.Connected(billingClient)
                } else {
                    statusStateFlow.value = Status.Unavailable()
                }
            }

            override fun onBillingServiceDisconnected() {
                statusStateFlow.value = Status.Disconnected()
                coroutineScope.launch {
                    delay(retryConnectionDelay)
                    connectBillingClient()
                }
            }
        })
    }

    sealed class Status {
        class Connected(val billingClient: BillingClient) : Status()
        class Unavailable : Status()
        class Disconnected : Status()
    }
}