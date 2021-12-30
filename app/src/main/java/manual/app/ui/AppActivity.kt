package manual.app.ui

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import manual.app.R
import manual.app.ads.GDPRHelper
import manual.app.ads.RewardedVideoManager
import manual.app.databinding.AppActivityBinding
import manual.app.premium.PremiumManager
import manual.app.repository.AppBackgroundsRepository
import manual.app.repository.MonetizationConfigRepository
import manual.core.activity.CoreActivity
import manual.core.coroutines.flow.launchWith
import manual.core.fragment.FragmentFactoryStore
import manual.core.fragment.setFactory
import manual.core.koin.attachKoinModule
import org.koin.android.ext.android.inject

class AppActivity : CoreActivity<AppActivityBinding>(AppActivityBinding::inflate) {

    private val fontScaleManager: FontScaleManager by inject()
    private val appBackgroundsRepository: AppBackgroundsRepository by inject()
    private val nightModeManager: NightModeManager by inject()
    private val premiumManager: PremiumManager by inject()
    private val monetizationConfigRepository: MonetizationConfigRepository by inject()
    private val preferences by lazy { getSharedPreferences("AppActivity", MODE_PRIVATE) }

    private var reviewRequested
        get() = preferences.getBoolean("reviewRequested", false)
        set(value) = preferences.edit { putBoolean("reviewRequested", value) }

    private var openCount
        get() = preferences.getInt("openCount", 0)
        set(value) = preferences.edit { putInt("openCount", value) }

    override fun getThemeResourceId() = R.style.Activity

    init {
        attachKoinModule {
            single { GDPRHelper(this@AppActivity) }
            single { RewardedVideoManager(this@AppActivity, get()) }
        }
    }

    override fun FragmentFactoryStore.setup() {
        setFactory { PremiumOfferFragment(FullVersionOfferFragmentDelegate()) }
        setFactory { ChaptersFragment(ChaptersFragmentDelegate()) }
        setFactory { ChapterFragment(ChapterFragmentDelegate()) }
        setFactory { ChestFragment(ChestFragmentDelegate()) }
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
                    root.setBackgroundResource(android.R.color.background_light)
                }
                AppBackgroundsRepository.nightAppBackground -> {
                    root.setBackgroundResource(android.R.color.background_dark)
                }
                else -> {
                    Glide.with(root).load(Uri.parse("file:///android_asset/${appBackground.source}")).listener(
                        object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                return true
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                root.background = resource
                                return true
                            }
                        }
                    ).submit()
                }
            }

            if (appBackground.nightMode) {
                nightModeManager.mode = NightModeManager.Mode.NIGHT
            } else {
                nightModeManager.mode = NightModeManager.Mode.NOT_NIGHT
            }

            if (appBackground != AppBackgroundsRepository.lightAppBackground) {
                window.statusBarColor = Color.BLACK
                window.navigationBarColor = Color.BLACK
            } else {
                window.statusBarColor = Color.WHITE
                window.navigationBarColor = Color.WHITE
            }
        }

        if (!isRecreated) {
            navigate<ChaptersFragment>(addToBackStack = false)
        }

        if (!reviewRequested) {
            combine(
                premiumManager.premiumEnabledFlow().filterNotNull(),
                monetizationConfigRepository.monetizationConfigFlow()
            ) { premiumEnabled, config ->
                val minOpenCount = when {
                    premiumEnabled || !config.restrictContents && !config.restrictChapters -> 5
                    else -> 15
                }

                if (!reviewRequested && openCount >= minOpenCount) {
                    requestReview()
                }
            }.launchWith(this@AppActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        openCount++
    }

    private fun requestReview() = with(ReviewManagerFactory.create(this)) {
        requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                launchReviewFlow(this@AppActivity, request.result).addOnCompleteListener {
                    reviewRequested = true
                }
            }
        }
    }

    inline fun <reified T : Fragment> navigate(
        arguments: Bundle? = null,
        addToBackStack: Boolean = true,
        tag: String? = T::class.java.name
    ) = supportFragmentManager.commit {
        if (addToBackStack) addToBackStack(null)
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
            if (chapterId == -1) {
                navigate<ChestFragment>()
            } else {
                navigate<ChapterFragment>(bundleOf(ChapterFragment.Argument.Int.CHAPTER_ID to chapterId))
            }
        }
    }

    private inner class ChaptersFragmentDelegate : ChaptersFragment.Delegate {
        override fun navigateToChapter(fragment: ChaptersFragment, chapterId: Int) {
            if (chapterId == -1) {
                navigate<ChestFragment>()
            } else {
                navigate<ChapterFragment>(bundleOf(ChapterFragment.Argument.Int.CHAPTER_ID to chapterId))
            }
        }

        override fun navigateToSettings(fragment: ChaptersFragment) {
            navigate<SettingsFragment>()
        }
    }

    private inner class FullVersionOfferFragmentDelegate : PremiumOfferFragment.Delegate {
        override fun onPremiumPurchased(fragment: PremiumOfferFragment) {
            supportFragmentManager.popBackStack()
        }
    }

    private inner class ChestFragmentDelegate : ChestFragment.Delegate {
        override fun navigateToChapter(fragment: ChestFragment, chapterId: Int) {
            if (chapterId == -1) {
                navigate<ChestFragment>()
            } else {
                navigate<ChapterFragment>(bundleOf(ChapterFragment.Argument.Int.CHAPTER_ID to chapterId))
            }
        }
    }
}