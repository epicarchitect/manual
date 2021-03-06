package manual.app.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.ads.consent.*
import manual.app.R
import java.net.URL

class GDPRHelper(private val context: Context) {

    private var form: ConsentForm? = null
    private val consentInformation = ConsentInformation.getInstance(context)

    val isEEA: Boolean
        get() = consentInformation.isRequestLocationInEeaOrUnknown

    val consentStatus: ConsentStatus
        get() = consentInformation.consentStatus

    fun checkConsent(activity: Activity, onStatusChangeListener: OnStatusChangeListener) {
        val publisherIds = arrayOf(context.getString(R.string.admob_publisher_id))
        consentInformation.requestConsentInfoUpdate(publisherIds, object : ConsentInfoUpdateListener {
            override fun onConsentInfoUpdated(status: ConsentStatus) {
                if (isEEA) {
                    if (status === ConsentStatus.UNKNOWN) {
                        openConsentDialog(activity, onStatusChangeListener)
                    } else {
                        onStatusChangeListener.onChange(status)
                    }
                } else {
                    onStatusChangeListener.onChange(status)
                }
            }

            override fun onFailedToUpdateConsentInfo(errorDescription: String) {
                Toast.makeText(activity, errorDescription, Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun openConsentDialog(activity: Activity, onStatusChangeListener: OnStatusChangeListener) {
        try {
            form = ConsentForm.Builder(activity, URL(context.getString(R.string.privacyPolicy_url)))
                .withListener(object : ConsentFormListener() {
                    override fun onConsentFormLoaded() {
                        form!!.show()
                    }

                    override fun onConsentFormClosed(status: ConsentStatus?, userPrefersAdFree: Boolean?) {
                        onStatusChangeListener.onChange(status)
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build()

            form!!.load()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun interface OnStatusChangeListener {
        fun onChange(status: ConsentStatus?)
    }
}