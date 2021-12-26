package manual.app.premium

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails

class BillingLauncher(
    private val billingClient: BillingClient,
    private val skuDetails: SkuDetails,
) {

    val formattedPrice get() = skuDetails.price

    fun launch(activity: Activity) {
        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        )
    }
}