package manual.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import epicarchitect.recyclerview.EpicAdapter
import epicarchitect.recyclerview.EpicAdapterBuilder
import epicarchitect.recyclerview.bind
import epicarchitect.recyclerview.requireEpicAdapter
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import manual.app.R
import manual.app.ads.NativeAdsManager
import manual.app.ads.RewardedAdManager
import manual.app.databinding.*
import manual.app.viewmodel.ChapterViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.CoreFragment
import manual.core.os.require
import manual.core.view.handleLinks
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChapterFragment(
    private val delegate: Delegate
) : CoreFragment<ChapterFragmentBinding>(ChapterFragmentBinding::inflate) {

    private val chapterId: Int get() = arguments.require(Argument.Int.CHAPTER_ID)
    private val viewModel: ChapterViewModel by viewModel { parametersOf(chapterId) }
    private val nativeAdsManager: NativeAdsManager by inject()
    private val audioAssetPlayer: AudioAssetPlayer by inject()
    private val rewardedAdManager: RewardedAdManager by inject()

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun ChapterFragmentBinding.onCreated() {
        unlockByAdOfferBlockImageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = resources.displayMetrics.heightPixels / 2
        }

        premiumOfferBlockImageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = resources.displayMetrics.heightPixels / 2
        }

        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        tagsRecyclerView.adapter = EpicAdapter {
            setup<ChapterViewModel.Tag, TagItemBinding>(TagItemBinding::inflate) {
                bind { item ->
                    chip.text = item.name
                }
            }
        }

        contentsRecyclerView.itemAnimator = null
        contentsRecyclerView.adapter = EpicAdapter {
            setupHtmlContent()
            setupImageContent()
            setupAudioContent()
            setupVideoContent()
            setupAdItem()
        }

        viewModel.state.map { it == null }.onEachChanged {
            progressBar.isVisible = it
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.title }.onEachChanged {
            titleTextView.text = it
        }.launchWith(viewLifecycleOwner)

        combine(
            viewModel.state.map { it?.isFavorite }.distinctUntilChanged(),
            viewModel.state.map { it?.isBlocked }.distinctUntilChanged(),
            viewModel.state.map { it?.canUnblockByAd }.distinctUntilChanged(),
        ) { isFavorite, isBlocked, canUnblockByAd ->
            when {
                isBlocked == null || isFavorite == null || canUnblockByAd == null -> {
                    favoriteImageView.isVisible = false
                    favoriteImageView.setOnClickListener(null)
                }

                isBlocked -> {
                    if (canUnblockByAd) {
                        favoriteImageView.setImageResource(R.drawable.ic_unblock_key)
                        favoriteImageView.isVisible = true
                        favoriteImageView.isEnabled = true
                        favoriteImageView.setOnClickListener(null)
                    } else {
                        favoriteImageView.setImageResource(R.drawable.ic_lock)
                        favoriteImageView.isVisible = true
                        favoriteImageView.isEnabled = false
                        favoriteImageView.setOnClickListener(null)
                    }
                }

                else -> {
                    favoriteImageView.setImageResource(
                        if (isFavorite) R.drawable.ic_star_checked
                        else R.drawable.ic_star_unchecked
                    )

                    favoriteImageView.setOnClickListener {
                        viewModel.setFavorite(!isFavorite)
                    }

                    favoriteImageView.isVisible = true
                    favoriteImageView.isEnabled = true
                }
            }
        }.launchWith(viewLifecycleOwner)

        premiumOfferLearnMoreButton.setOnClickListener {
            delegate.navigateToPremiumOffer(this@ChapterFragment)
        }

        unlockByAdOfferRemoveAdsButton.setOnClickListener {
            delegate.navigateToPremiumOffer(this@ChapterFragment)
        }

        unlockByAdOfferShowAdButton.setOnClickListener {
            rewardedAdManager.showRewardedVideo(
                requireActivity(),
                object : RewardedAdManager.RewardedVideoCallback {
                    override fun onReward() {
                        viewModel.unblock()
                    }
                }
            )
        }

        combine(
            viewModel.state.map { it?.isBlocked }.distinctUntilChanged(),
            viewModel.state.map { it?.canUnblockByAd }.distinctUntilChanged(),
            rewardedAdManager.isLoadedFlow()
        ) { isBlocked, canUnblockByAd, isRewardedAdLoaded ->
            when {
                isBlocked == true && canUnblockByAd == false -> {
                    premiumOfferLayout.isVisible = true
                    contentsRecyclerView.isVisible = true
                    unlockByAdOfferLayout.isVisible = false
                    scrollView.setOnTouchListener { _, _ -> true }
                    contentsRecyclerView.setOnTouchListener { _, _ -> true }
                }

                isBlocked == true && canUnblockByAd == true -> {
                    if (isRewardedAdLoaded) {
                        unlockByAdOfferLayout.isVisible = true
                        premiumOfferLayout.isVisible = false
                    } else {
                        unlockByAdOfferLayout.isVisible = false
                        premiumOfferLayout.isVisible = true
                    }
                    scrollView.setOnTouchListener { _, _ -> true }
                    contentsRecyclerView.setOnTouchListener { _, _ -> true }
                }

                else -> {
                    unlockByAdOfferLayout.isVisible = false
                    premiumOfferLayout.isVisible = false
                    contentsRecyclerView.isVisible = true
                    scrollView.setOnTouchListener { _, _ -> false }
                    contentsRecyclerView.setOnTouchListener { _, _ -> false }
                }
            }
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.contents }.onEachChanged {
            contentsRecyclerView.requireEpicAdapter().loadItems(it ?: emptyList())
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.tags }.onEachChanged {
            tagsRecyclerView.requireEpicAdapter().loadItems(it ?: emptyList())
        }.launchWith(viewLifecycleOwner)
    }

    private fun EpicAdapterBuilder.setupHtmlContent() =
        setup<ChapterViewModel.Content.Html, ChapterTextItemBinding>(ChapterTextItemBinding::inflate) {
            bind { item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                textView.movementMethod = BetterLinkMovementMethod.getInstance()
                textView.text = item.html.handleLinks {
                    if (it.scheme == "manual") {
                        delegate.navigateToChapter(
                            this@ChapterFragment,
                            it.pathSegments.last().toInt()
                        )
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, it))
                    }
                }
            }
        }

    private fun EpicAdapterBuilder.setupImageContent() =
        setup<ChapterViewModel.Content.Image, ChapterImageItemBinding>(ChapterImageItemBinding::inflate) {
            bind { item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                Glide.with(root).load(item.uri).into(imageView)
                imageView.setOnClickListener {
                    ImageActivity.open(
                        context = it.context,
                        uri = item.uri,
                        title = item.name
                    )
                }
            }
        }

    private fun EpicAdapterBuilder.setupVideoContent() =
        setup<ChapterViewModel.Content.Video, ChapterVideoItemBinding>(ChapterVideoItemBinding::inflate) {
            bind { item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name

                playerView.player = ExoPlayer.Builder(root.context).build().apply {
                    playWhenReady = true
                    repeatMode = ExoPlayer.REPEAT_MODE_ALL
                    setMediaItem(
                        MediaItem.fromUri(
                            Uri.parse("asset:///${item.source}")
                        )
                    )
                    prepare()
                    play()
                }

                try {
                    awaitCancellation()
                } catch (e: Exception) {
                    playerView.player?.release()
                    playerView.player = null
                }
            }
        }

    @SuppressLint("SetTextI18n")
    private fun EpicAdapterBuilder.setupAudioContent() =
        setup<ChapterViewModel.Content.Audio, ChapterAudioItemBinding>(ChapterAudioItemBinding::inflate) {
            bind { scope, _, item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                audioAssetPlayer.state.onEach {
                    if (it?.path == item.source) {
                        if (it.isPlaying) {
                            playButton.setImageResource(R.drawable.ic_pause)
                            playButton.setOnClickListener {
                                audioAssetPlayer.pause()
                            }
                        } else {
                            playButton.setImageResource(R.drawable.ic_play)
                            playButton.setOnClickListener {
                                audioAssetPlayer.resume()
                            }
                        }

                        seekBar.isEnabled = true
                        seekBar.max = it.duration
                        seekBar.progress = it.position
                        seekBar.setOnSeekBarChangeListener(
                            object : SeekBar.OnSeekBarChangeListener {
                                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                                override fun onProgressChanged(
                                    seekBar: SeekBar,
                                    progress: Int,
                                    fromUser: Boolean
                                ) {
                                    if (fromUser) {
                                        audioAssetPlayer.seekTo(progress)
                                    }
                                }
                            }
                        )

                        positionTextView.isVisible = true
                        durationTextView.isVisible = true
                        durationDividerTextView.isVisible = true
                        positionTextView.text = formatAudioTime(it.position)
                        durationTextView.text = formatAudioTime(it.duration)
                    } else {
                        seekBar.setOnSeekBarChangeListener(null)
                        seekBar.isEnabled = false
                        seekBar.progress = 0
                        playButton.setImageResource(R.drawable.ic_play)
                        playButton.setOnClickListener {
                            audioAssetPlayer.play(item.source)
                        }
                        positionTextView.isVisible = false
                        durationTextView.isVisible = false
                        durationDividerTextView.isVisible = false
                    }
                }.launchIn(scope)
            }
        }


    private fun EpicAdapterBuilder.setupAdItem() =
        setup<ChapterViewModel.Content.NativeAd, NativeAdItemBinding>(NativeAdItemBinding::inflate) {
            bind { _ ->
                val nativeAd = nativeAdsManager.randomNativeAd()

                if (nativeAd == null) {
                    root.updateLayoutParams {
                        height = 0
                    }
                    return@bind
                } else {
                    root.updateLayoutParams {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }

                premiumButton.setOnClickListener {
                    delegate.navigateToPremiumOffer(this@ChapterFragment)
                }

                adView.mediaView = adMediaView
                adView.headlineView = adHeadlineTextView
                adView.bodyView = adBodyTextView
                adView.callToActionView = adActionButton
                adView.iconView = adIconImageView
                adView.priceView = adPriceTextView
                adView.starRatingView = ratingBar
                adView.storeView = adStoreTextView
                adView.advertiserView = adAdvertiserTextView

                // The headline and media content are guaranteed to be in every UnifiedNativeAd.
                (adView.headlineView as TextView).text = nativeAd.headline
                adView.mediaView!!.setMediaContent(nativeAd.mediaContent!!)

                // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
                // check before trying to display them.
                if (nativeAd.body == null) {
                    adView.bodyView!!.visibility = View.INVISIBLE
                } else {
                    adView.bodyView!!.visibility = View.VISIBLE
                    (adView.bodyView as TextView).text = nativeAd.body
                }

                if (nativeAd.callToAction == null) {
                    adView.callToActionView!!.visibility = View.INVISIBLE
                } else {
                    adView.callToActionView!!.visibility = View.VISIBLE
                    (adView.callToActionView as Button).text = nativeAd.callToAction
                }

                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(nativeAd.icon!!.drawable)
                    adView.iconView!!.visibility = View.VISIBLE
                }

                if (nativeAd.price == null) {
                    adView.priceView!!.visibility = View.INVISIBLE
                } else {
                    adView.priceView!!.visibility = View.VISIBLE
                    (adView.priceView as TextView).text = nativeAd.price
                }

                if (nativeAd.store == null) {
                    adView.storeView!!.visibility = View.INVISIBLE
                } else {
                    adView.storeView!!.visibility = View.VISIBLE
                    (adView.storeView as TextView).text = nativeAd.store
                }

                if (nativeAd.starRating == null) {
                    adView.starRatingView!!.visibility = View.INVISIBLE
                } else {
                    (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                    adView.starRatingView!!.visibility = View.VISIBLE
                }

                if (nativeAd.advertiser == null) {
                    adView.advertiserView!!.visibility = View.INVISIBLE
                } else {
                    (adView.advertiserView as TextView).text = nativeAd.advertiser
                    adView.advertiserView!!.visibility = View.VISIBLE
                }

                // This method tells the Google Mobile Ads SDK that you have finished populating your
                // native ad view with this native ad.
                adView.setNativeAd(nativeAd)
            }
        }

    private fun formatAudioTime(time: Int): String = buildString {
        val seconds = time / 1000 % 60
        val minutes = time / 1000 / 60

        append(minutes)
        append(":")
        if (seconds < 10) {
            append(0)
        }
        append(seconds)
    }

    object Argument {
        object Int {
            const val CHAPTER_ID = "chapterId"
        }
    }

    interface Delegate {
        fun navigateToChapter(fragment: ChapterFragment, chapterId: Int)
        fun navigateToPremiumOffer(fragment: ChapterFragment)
    }
}