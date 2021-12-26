package manual.app.ui

import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import kotlinx.coroutines.flow.onEach
import manual.app.R
import manual.core.coroutines.flow.launchWith
import manual.app.databinding.PremiumOfferFragmentBinding
import manual.core.fragment.CoreFragment
import manual.app.premium.PremiumManager
import manual.core.resources.read
import org.koin.android.ext.android.inject

class PremiumOfferFragment(
    private val delegate: Delegate
) : CoreFragment<PremiumOfferFragmentBinding>(PremiumOfferFragmentBinding::inflate) {

    private val premiumManager: PremiumManager by inject()

    override fun PremiumOfferFragmentBinding.onCreated() {
        descriptionTextView.text = HtmlCompat.fromHtml(
            requireContext().assets.read("monetization/premium-description.html"),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        premiumManager.billingLauncherFlow().onEach { billingLauncher ->
            TransitionManager.beginDelayedTransition(root)
            priceTextView.text = billingLauncher?.formattedPrice ?: ""
            progressBar.isVisible = billingLauncher == null
            priceTitleTextView.isVisible = billingLauncher != null
            priceTextView.isVisible = billingLauncher != null
            buyButton.isVisible = billingLauncher != null
            buyButton.setOnClickListener {
                billingLauncher?.launch(requireActivity())
            }
        }.launchWith(viewLifecycleOwner)

        premiumManager.premiumEnabledFlow().onEach {
            if (it == true) {
                delegate.onPremiumPurchased(this@PremiumOfferFragment)
                Toast.makeText(requireContext(), R.string.premiumOfferFragment_finished, Toast.LENGTH_SHORT).show()
            }
        }.launchWith(viewLifecycleOwner)
    }

    interface Delegate {
        fun onPremiumPurchased(fragment: PremiumOfferFragment)
    }
}