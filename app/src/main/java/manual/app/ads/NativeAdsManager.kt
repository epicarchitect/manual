package manual.app.ads

import android.content.Context
import androidx.core.os.bundleOf
import com.google.ads.consent.ConsentStatus
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import manual.app.R
import kotlin.random.Random

class NativeAdsManager(
    private val context: Context,
    private val gdprHelper: GDPRHelper
) {

    private val nativeAds = mutableListOf<NativeAd>()
    private val isPersonalized get() = gdprHelper.isEEA && gdprHelper.consentStatus == ConsentStatus.PERSONALIZED
    private val random = Random(System.currentTimeMillis())

    init {
        load()
    }

    fun load() {
        AdLoader.Builder(context, context.getString(R.string.admob_nativeAd_id)).forNativeAd {
            nativeAds.add(it)
        }.build().loadAds(buildRequest(isPersonalized),10)
    }

    fun randomNativeAd() = try {
        nativeAds[random.nextInt(nativeAds.size)]
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun buildRequest(isPersonalized: Boolean) = AdRequest.Builder().apply {
        if (!isPersonalized) {
            addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                bundleOf("npa" to "1")
            )
        }
    }.build()
}