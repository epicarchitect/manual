package manual.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import manual.app.R
import manual.app.ads.NativeAdsManager
import manual.app.databinding.*
import manual.app.viewmodel.ChapterViewModel
import manual.app.viewmodel.ChaptersViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.CoreFragment
import manual.core.os.require
import manual.core.view.*
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

    @SuppressLint("SetTextI18n")
    override fun ChapterFragmentBinding.onCreated() {
        tagsRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setup<ChapterViewModel.Tag, TagItemBinding>(TagItemBinding::inflate) {
                bind { item ->
                    chip.text = item.name
                }
            }
        }

        contentsRecyclerView.itemAnimator = null
        contentsRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setupHtmlContent()
            setupImageContent()
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
            viewModel.state.map { it?.isBlocked }.distinctUntilChanged()
        ) { isFavorite, isBlocked ->
            when {
                isBlocked == null || isFavorite == null -> {
                    favoriteImageView.isVisible = false
                    favoriteImageView.setOnClickListener(null)
                }
                isBlocked -> {
                    favoriteImageView.setImageResource(R.drawable.ic_lock)
                    favoriteImageView.isVisible = true
                    favoriteImageView.isEnabled = false
                    favoriteImageView.setOnClickListener(null)
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

        viewModel.state.map { it?.isBlocked }.onEachChanged {
            premiumOfferLayout.isVisible = it == true
            contentsRecyclerView.isVisible = it == false
            divider.isVisible = it == false
            if (it == true) {
                premiumOfferLearnMoreButton.setOnClickListener {
                    delegate.navigateToPremiumOffer(this@ChapterFragment)
                }
            } else {
                premiumOfferLearnMoreButton.setOnClickListener(null)
            }
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.contents }.onEachChanged {
            contentsRecyclerView.requireBindingRecyclerViewAdapter().loadItems(it ?: emptyList())
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.tags }.onEachChanged {
            tagsRecyclerView.requireBindingRecyclerViewAdapter().loadItems(it ?: emptyList())
        }.launchWith(viewLifecycleOwner)
    }

    private fun BindingRecyclerViewAdapterBuilder.setupHtmlContent() =
        setup<ChapterViewModel.Content.Html, ChapterTextItemBinding>(ChapterTextItemBinding::inflate) {
            bind { item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                textView.movementMethod = BetterLinkMovementMethod.getInstance()
                textView.text = item.html.handleLinks {
                    if (it.scheme == "manual") {
                        delegate.navigateToChapter(this@ChapterFragment, it.pathSegments.last().toInt())
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, it))
                    }
                }
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupImageContent() =
        setup<ChapterViewModel.Content.Image, ChapterImageItemBinding>(ChapterImageItemBinding::inflate) {
            bind { item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                Glide.with(root).load(item.uri).into(imageView)
                imageView.setOnClickListener {
                    StfalconImageViewer.Builder(context, listOf(item.uri)) { view, uri ->
                        Glide.with(root).load(uri).into(view)
                    }.withHiddenStatusBar(false).show()
                }
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupAdItem() =
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