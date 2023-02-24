package manual.app.premium

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.querySkuDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import manual.app.R

class PremiumManager(
    context: Context,
    billingClientManager: BillingClientManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val premiumProductId = context.getString(R.string.premium_productId)
    private val billingClientStateFlow = MutableStateFlow<BillingClient?>(null)
    private val skuDetailsStateFlow = MutableStateFlow<SkuDetails?>(null)
    private val billingLauncherStateFlow = MutableStateFlow<BillingLauncher?>(null)
    private val billingResultStateFlow = MutableStateFlow<BillingResult?>(null)
    private val premiumEnabledStateFlow = MutableStateFlow<Boolean?>(null)

    init {
        billingClientManager.statusFlow().onEach {
            when (it) {
                is BillingClientManager.Status.Connected -> {
                    billingClientStateFlow.value = it.billingClient
                }
                is BillingClientManager.Status.Unavailable -> {
                    if (premiumEnabledStateFlow.value == null) {
                        premiumEnabledStateFlow.value = false
                    }

                    billingClientStateFlow.value = null
                }
                else -> {
                    // nothing
                }
            }
        }.launchIn(coroutineScope)

        billingClientStateFlow.onEach {
            billingClientStateFlow.value = it
            skuDetailsStateFlow.value = null

            if (it != null) {
                skuDetailsStateFlow.value = it.querySkuDetails(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(listOf(premiumProductId))
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                ).skuDetailsList?.firstOrNull()

                checkFullVersion(it)
            }
        }.launchIn(coroutineScope)

        billingClientStateFlow.combine(skuDetailsStateFlow) { billingClient, skuDetails ->
            if (billingClient == null || skuDetails == null) {
                billingLauncherStateFlow.value = null
            } else {
                billingLauncherStateFlow.value = BillingLauncher(billingClient, skuDetails)
            }
        }.launchIn(coroutineScope)

        billingClientManager.purchaseResultFlow()
            .combine(billingClientStateFlow) { billingResult, billingClient ->
                val purchases = billingResult?.second

                billingResultStateFlow.value = billingResult?.first
                if (purchases != null && purchases.isNotEmpty() && billingClient != null) {
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchases.first().purchaseToken)
                            .build()
                    )

                    checkFullVersion(billingClient)
                }
            }.launchIn(coroutineScope)
    }

    private fun checkFullVersion(client: BillingClient) {
        client.queryPurchasesAsync(BillingClient.SkuType.INAPP) { _, purchases ->
            var isPurchased = false
            for (purchase in purchases) {
                if (purchase.skus.contains(premiumProductId)) {
                    isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    break
                }
            }

            premiumEnabledStateFlow.value = isPurchased
        }
    }

    fun billingLauncherFlow(): Flow<BillingLauncher?> = billingLauncherStateFlow

    fun premiumEnabledFlow(): Flow<Boolean?> = premiumEnabledStateFlow

}