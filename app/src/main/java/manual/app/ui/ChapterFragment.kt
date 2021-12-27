package manual.app.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import manual.app.R
import manual.app.ads.RewardedVideoManager
import manual.app.databinding.*
import manual.app.viewmodel.ChapterViewModel
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
    private val rewardedVideoManager: RewardedVideoManager by inject()

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

                if (item.isBlocked) {
                    unblockLayout.isVisible = true
                    buyButton.setOnClickListener {
                        delegate.navigateToPremiumOffer(this@ChapterFragment)
                    }

                    if (item.canUnblockByAds) {
                        orTextView.isVisible = true
                        showAdButton.isVisible = true
                        showAdButton.setOnClickListener {
                            rewardedVideoManager.showRewardedVideo(object : RewardedVideoManager.RewardedVideoCallback {
                                override fun onReward() {
                                    viewModel.unblockContent(item.contentId)
                                }

                                override fun onFailed(code: Int, message: String?) {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    } else {
                        orTextView.isVisible = false
                        showAdButton.isVisible = false
                        showAdButton.setOnClickListener(null)
                    }
                } else {
                    unblockLayout.isVisible = false
                    buyButton.setOnClickListener(null)
                    showAdButton.setOnClickListener(null)
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