package manual.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import epicarchitect.recyclerview.EpicAdapter
import epicarchitect.recyclerview.requireEpicAdapter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import manual.app.R
import manual.app.ads.GDPRHelper
import manual.app.data.AppBackground
import manual.app.databinding.BgItemBinding
import manual.app.databinding.SettingsFragmentBinding
import manual.app.repository.AppBackgroundsRepository
import manual.app.repository.MonetizationConfigRepository
import manual.core.coroutines.flow.launchWith
import manual.core.fragment.CoreFragment
import org.koin.android.ext.android.inject

class SettingsFragment : CoreFragment<SettingsFragmentBinding>(SettingsFragmentBinding::inflate) {

    private val fontScaleManager: FontScaleManager by inject()
    private val nightModeManager: NightModeManager by inject()
    private val gdprHelper: GDPRHelper by inject()
    private val appBackgroundsRepository: AppBackgroundsRepository by inject()
    private val monetizationConfigRepository: MonetizationConfigRepository by inject()

    override fun SettingsFragmentBinding.onCreated() {
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        fontSizeSeekBar.progress = ((fontScaleManager.fontScale - 1.0f) * 10).toInt()
        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) =
                Unit

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                fontScaleManager.fontScale = 1.0f + seekBar.progress.toFloat() / 10
                requireActivity().recreate()
            }
        })


        bgRecyclerView.adapter = EpicAdapter {
            setup<AppBackground, BgItemBinding>(BgItemBinding::inflate) {
                bind { scope, _, item ->
                    when (item) {
                        AppBackgroundsRepository.lightAppBackground -> {
                            imageView.setImageResource(android.R.color.background_light)
                        }
                        AppBackgroundsRepository.nightAppBackground -> {
                            imageView.setImageResource(android.R.color.background_dark)
                        }
                        else -> {
                            Glide.with(root).load(Uri.parse("file:///android_asset/${item.source}"))
                                .into(imageView)
                        }
                    }

                    appBackgroundsRepository.currentAppBackgroundFlow().onEach {
                        root.isChecked = item == it
                    }.launchIn(scope)

                    root.setOnClickListener {
                        appBackgroundsRepository.setCurrentAppBackground(item.id)
                        if (item.nightMode) {
                            nightModeManager.mode = NightModeManager.Mode.NIGHT
                        } else {
                            nightModeManager.mode = NightModeManager.Mode.NOT_NIGHT
                        }
                        requireActivity().recreate()
                    }
                }
            }
        }

        appBackgroundsRepository.appBackgroundsFlow().onEach {
            bgRecyclerView.requireEpicAdapter().loadItems(it)
        }.launchWith(viewLifecycleOwner)

        monetizationConfigRepository.monetizationConfigFlow().onEach {
            val isAdsShowing = it.showInterstitialAds || it.showNativeAds
            changeGdprButton.isVisible = isAdsShowing && gdprHelper.isEEA
            gdprDescriptionTextView.isVisible = isAdsShowing && gdprHelper.isEEA
            changeGdprButton.setOnClickListener {
                gdprHelper.openConsentDialog(requireActivity()) {}
            }
        }.launchWith(viewLifecycleOwner)

        reviewButton.setOnClickListener {
            goToAppInStore()
        }
    }

    fun goToAppInStore() = with(requireActivity()) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                R.string.chapters_openPlayMarketFailed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}