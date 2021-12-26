package manual.app.ads

import android.app.Activity
import com.google.ads.consent.*
import manual.app.R
import java.net.URL

class GDPRHelper(private val activity: Activity) {

    private var form: ConsentForm? = null
    private val consentInformation = ConsentInformation.getInstance(activity)

    val isEEA: Boolean
        get() = consentInformation.isRequestLocationInEeaOrUnknown

    val consentStatus: ConsentStatus
        get() = consentInformation.consentStatus

    fun checkConsent(onStatusChangeListener: OnStatusChangeListener) {
        val publisherIds = arrayOf(activity.getString(R.string.admob_publisher_id))
        consentInformation.requestConsentInfoUpdate(publisherIds, object : ConsentInfoUpdateListener {
            override fun onConsentInfoUpdated(status: ConsentStatus) {
                if (isEEA) {
                    if (status === ConsentStatus.UNKNOWN) {
                        openConsentDialog(onStatusChangeListener)
                    } else {
                        onStatusChangeListener.onChange(status)
                    }
                } else {
                    onStatusChangeListener.onChange(status)
                }
            }

            override fun onFailedToUpdateConsentInfo(errorDescription: String) {
            }
        })
    }

    fun openConsentDialog(onStatusChangeListener: OnStatusChangeListener) {
        try {
            form = ConsentForm.Builder(activity, URL(activity.getString(R.string.privacyPolicy_url)))
                .withListener(object : ConsentFormListener() {
                    override fun onConsentFormLoaded() {
                        form?.show()
                    }

                    override fun onConsentFormClosed(status: ConsentStatus?, userPrefersAdFree: Boolean?) {
                        onStatusChangeListener.onChange(status)
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build()

            checkNotNull(form).load()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun interface OnStatusChangeListener {
        fun onChange(status: ConsentStatus?)
    }
}