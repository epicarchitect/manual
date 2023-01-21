package manual.app.ui

import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import manual.app.R
import manual.app.ads.InterstitialAdManager
import manual.app.ads.RewardedAdManager
import manual.app.data.Note
import manual.app.databinding.AppActivityBinding
import manual.app.premium.PremiumManager
import manual.app.repository.AppBackgroundsRepository
import manual.app.repository.MonetizationConfigRepository
import manual.core.activity.CoreActivity
import manual.core.coroutines.flow.launchWith
import manual.core.fragment.FragmentFactoryStore
import manual.core.fragment.setFactory
import org.koin.android.ext.android.inject

class AppActivity : CoreActivity<AppActivityBinding>(AppActivityBinding::inflate) {

    private val fontScaleManager: FontScaleManager by inject()
    private val appBackgroundsRepository: AppBackgroundsRepository by inject()
    private val nightModeManager: NightModeManager by inject()
    private val premiumManager: PremiumManager by inject()
    private val appUpdateManager: AppUpdateManager by inject()
    private val reviewManager: ReviewManager by inject()
    private val monetizationConfigRepository: MonetizationConfigRepository by inject()
    private val interstitialAdManager: InterstitialAdManager by inject()
    private val rewardedAdManager: RewardedAdManager by inject()
    private val alertDialogManager: AlertDialogManager by inject()
    private val preferences by lazy { getSharedPreferences("AppActivity", MODE_PRIVATE) }
    private var showInterstitialAds: Boolean? = null
    private var chapterCloseCount = 0

    private var reviewRequested
        get() = preferences.getBoolean("reviewRequested", false)
        set(value) = preferences.edit { putBoolean("reviewRequested", value) }

    private var openCount
        get() = preferences.getInt("openCount", 0)
        set(value) = preferences.edit { putInt("openCount", value) }

    private val installStateUpdatedListener = InstallStateUpdatedListener {
        if (it.installStatus() == InstallStatus.DOWNLOADED) {
            Snackbar.make(
                requireBinding().root,
                R.string.app_updateDownloaded_description,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.app_completeUpdate_button) {
                appUpdateManager.completeUpdate()
            }.show()
        }
    }

    override fun getThemeResourceId() = R.style.Activity

    override fun FragmentFactoryStore.setup() {
        setFactory { PremiumOfferFragment(FullVersionOfferFragmentDelegate()) }
        setFactory { ChaptersFragment(ChaptersFragmentDelegate()) }
        setFactory { ChapterFragment(ChapterFragmentDelegate()) }
        setFactory { NotesFragment(NotesFragmentDelegate()) }
        setFactory { NoteFragment(NoteFragmentDelegate()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        fontScaleManager.attachFontScale(this)
        super.onCreate(savedInstanceState)
    }

    override fun AppActivityBinding.onCreated() {
        runBlocking {
            val appBackground = appBackgroundsRepository.currentAppBackgroundFlow().first()

            when (appBackground) {
                AppBackgroundsRepository.lightAppBackground -> {
                    backgroundImageView.setBackgroundResource(android.R.color.background_light)
                }
                AppBackgroundsRepository.nightAppBackground -> {
                    backgroundImageView.setBackgroundResource(android.R.color.background_dark)
                }
                else -> {
                    Glide.with(backgroundImageView)
                        .load(Uri.parse("file:///android_asset/${appBackground.source}"))
                        .into(backgroundImageView)
                }
            }

            if (appBackground.nightMode) {
                nightModeManager.mode = NightModeManager.Mode.NIGHT
            } else {
                nightModeManager.mode = NightModeManager.Mode.NOT_NIGHT
            }
        }

        if (!isRecreated) {
            supportFragmentManager.commit {
                replace(
                    R.id.fragmentContainerView,
                    ChaptersFragment::class.java,
                    null
                )
            }
        }

        combine(
            premiumManager.premiumEnabledFlow().filterNotNull(),
            monetizationConfigRepository.monetizationConfigFlow()
        ) { premiumEnabled, config ->
            showInterstitialAds = !premiumEnabled && config.showInterstitialAds

            val minOpenCount = when {
                premiumEnabled || !config.showInterstitialAds && !config.restrictChapters -> 5
                else -> 15
            }

            if (!reviewRequested && openCount >= minOpenCount) {
                with(reviewManager) {
                    requestReviewFlow().addOnCompleteListener { request ->
                        if (request.isSuccessful) {
                            launchReviewFlow(
                                this@AppActivity,
                                request.result
                            ).addOnCompleteListener {
                                reviewRequested = true
                            }
                        }
                    }
                }
            }
        }.launchWith(this@AppActivity)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (appUpdateInfo.installStatus() != InstallStatus.DOWNLOADING) {
                    Snackbar.make(
                        root,
                        R.string.app_updateAvailable_description,
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.app_downloadUpdate_button) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this@AppActivity,
                            RequestCode.APP_UPDATE
                        )
                    }.show()
                }
            }
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            FragmentLifecycleCallbacks(),
            false
        )
    }

    override fun onResume() {
        super.onResume()
        openCount++
        requireBinding().darkView.animate().alpha(0f).setDuration(1000).start()
    }

    override fun onStart() {
        super.onStart()
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    override fun onStop() {
        super.onStop()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        rewardedAdManager.release()
    }

    inline fun <reified T : Fragment> navigate(
        arguments: Bundle? = null,
        tag: String? = T::class.java.name
    ) = supportFragmentManager.commit {
        addToBackStack(null)
        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        replace(
            R.id.fragmentContainerView,
            T::class.java,
            arguments,
            tag
        )
    }

    private inner class ChapterFragmentDelegate : ChapterFragment.Delegate {
        override fun navigateToPremiumOffer(fragment: ChapterFragment) {
            navigate<PremiumOfferFragment>()
        }

        override fun navigateToChapter(fragment: ChapterFragment, chapterId: Int) {
            navigate<ChapterFragment>(bundleOf(ChapterFragment.Argument.Int.CHAPTER_ID to chapterId))
        }
    }

    private inner class ChaptersFragmentDelegate : ChaptersFragment.Delegate {
        override fun navigateToChapter(fragment: ChaptersFragment, chapterId: Int) {
            navigate<ChapterFragment>(bundleOf(ChapterFragment.Argument.Int.CHAPTER_ID to chapterId))
        }

        override fun navigateToSettings(fragment: ChaptersFragment) {
            navigate<SettingsFragment>()
        }

        override fun navigateToPremiumOffer(fragment: ChaptersFragment) {
            navigate<PremiumOfferFragment>()
        }

        override fun navigateToNotes(fragment: ChaptersFragment) {
            navigate<NotesFragment>()
        }
    }

    private inner class FullVersionOfferFragmentDelegate : PremiumOfferFragment.Delegate {
        override fun onPremiumPurchased(fragment: PremiumOfferFragment) {
            supportFragmentManager.popBackStack()
        }
    }

    private inner class NotesFragmentDelegate : NotesFragment.Delegate {
        override fun onNoteCLick(note: Note) {
            navigate<NoteFragment>(NoteFragment.buildArguments(note.id))
        }

        override fun onAddNoteCLick() {
            navigate<NoteFragment>(NoteFragment.buildArguments(null))
        }
    }

    private inner class NoteFragmentDelegate : NoteFragment.Delegate {
        override fun onNoteDelete() {
            supportFragmentManager.popBackStack()
        }
    }

    private inner class FragmentLifecycleCallbacks : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentDestroyed(fragmentManager: FragmentManager, fragment: Fragment) {
            if (fragment is ChapterFragment) {
                chapterCloseCount++
                if (showInterstitialAds == true && chapterCloseCount % 3 == 0 && chapterCloseCount > 0) {
                    interstitialAdManager.show(this@AppActivity) {
                        alertDialogManager.showAlert(
                            context = this@AppActivity,
                            title = getString(R.string.removeAdsOffer_title),
                            message = getString(R.string.removeAdsOffer_description),
                            positiveButtonTitle = getString(R.string.removeAdsOffer_learMore_button),
                            onPositive = {
                                navigate<PremiumOfferFragment>()
                            }
                        )
                    }
                }
            }
        }
    }

    object RequestCode {
        const val APP_UPDATE = 12
    }
}