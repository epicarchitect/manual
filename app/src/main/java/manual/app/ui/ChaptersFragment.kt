package manual.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.Glide
import com.mctech.library.keyboard.visibilitymonitor.KeyboardVisibilityMonitor
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import manual.app.R
import manual.app.ads.NativeAdsManager
import manual.app.databinding.*
import manual.app.repository.ChapterGroupIconsRepository
import manual.app.repository.ChapterIconsRepository
import manual.app.viewmodel.ChaptersViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.CoreFragment
import manual.core.fragment.FragmentFactoryStore
import manual.core.fragment.instantiate
import manual.core.fragment.setFactory
import manual.core.resources.dp
import manual.core.view.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ChaptersFragment(private val delegate: Delegate) : CoreFragment<ChaptersFragmentBinding>(ChaptersFragmentBinding::inflate) {

    private val viewModel: ChaptersViewModel by sharedViewModel()
    private val nativeAdsManager: NativeAdsManager by inject()
    private val chapterIconsRepository: ChapterIconsRepository by inject()
    private val chapterGroupIconsRepository: ChapterGroupIconsRepository by inject()

    override fun FragmentFactoryStore.setup() {
        setFactory { TagSelectionBottomSheetDialogFragment(TagSelectionBottomSheetDialogFragmentDelegate()) }
    }

    @SuppressLint("SetTextI18n")
    override fun ChaptersFragmentBinding.onCreated() {
        settingsButton.setOnClickListener {
            delegate.navigateToSettings(this@ChaptersFragment)
        }

        KeyboardVisibilityMonitor(viewLifecycleOwner, activity as AppCompatActivity) {
            if (it.isOpened) {
                searchEditText.requestFocus()
            } else {
                searchEditText.clearFocus()
            }
        }

        searchEditText.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchEditText.hideKeyboard()
                return@OnEditorActionListener true
            }

            false
        })

        searchEditText.doAfterTextChanged { viewModel.setSearchText(it?.toString()) }

        clearSearchButton.setOnClickListener {
            viewModel.setSearchText(null)
        }

        chaptersRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setupChapterItem()
            setupGroupItem()
            setupFavoriteGroupItem()
            setupAdItem()
        }

        searchTypeSpinner.adapter = SearchTypeSpinnerAdapter(
            requireContext(),
            listOf(
                "Поиск по группам",
                "Поиск по тегам",
                "Поиск по названию",
                "Показывать только избранное"
            )
        )

        searchTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        viewModel.setSearchType(ChaptersViewModel.SearchType.BY_GROUPS)
                    }
                    1 -> {
                        viewModel.setSearchType(ChaptersViewModel.SearchType.BY_TAGS)
                    }
                    2 -> {
                        viewModel.setSearchType(ChaptersViewModel.SearchType.BY_NAME)
                    }
                    3 -> {
                        viewModel.setSearchType(ChaptersViewModel.SearchType.ONLY_FAVORITES)
                    }
                    else -> error("hm")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        tagsRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setup<ChaptersViewModel.Tag, TagItemBinding>(TagItemBinding::inflate) {
                bind { item ->
                    chip.text = item.name
                }
            }

            setup<SelectTagsButtonItem, SelectTagsButtonItemBinding>(SelectTagsButtonItemBinding::inflate) {
                bind { item ->
                    root.setOnClickListener {
                        fragmentFactoryStore.instantiate<TagSelectionBottomSheetDialogFragment>().apply {
                            arguments = TagSelectionBottomSheetDialogFragment.buildArguments(
                                (viewModel.state.value!!.searchState as ChaptersViewModel.SearchState.ByTags).tags.map { it.id }
                            )
                        }.show(childFragmentManager, null)
                    }
                }
            }
        }

        viewModel.state.map { it == null }.onEachChanged {
            progressBar.isVisible = it
        }.launchWith(viewLifecycleOwner)

        val backPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, false) {
            viewModel.navigateBack()
        }

        backButton.setOnClickListener {
            viewModel.navigateBack()
        }

        titleTextView.setOnClickListener {
            viewModel.navigateBack()
        }

        viewModel.state.map { it?.searchState }.onEachChanged { searchState ->
            when (searchState) {
                is ChaptersViewModel.SearchState.ByGroups -> {
                    searchTypeSpinner.setSelection(0)
                    searchEditText.isVisible = false
                    clearSearchButton.isVisible = false
                    tagsRecyclerView.isVisible = false
                    if (searchState.isRoot) {
                        titleTextView.text = getString(R.string.chapters_title)
                    } else {
                        titleTextView.text = searchState.currentGroupTitle
                    }
                    searchEditText.hideKeyboard()
                }
                is ChaptersViewModel.SearchState.ByName -> {
                    searchTypeSpinner.setSelection(2)
                    titleTextView.text = getString(R.string.chapters_title)
                    searchEditText.isVisible = true
                    clearSearchButton.isVisible = true
                    tagsRecyclerView.isVisible = false

                    if (searchEditText.text.toString() != searchState.name) {
                        searchEditText.setText(searchState.name)
                    }

                    clearSearchButton.isVisible = !searchState.name.isNullOrEmpty()
                }
                is ChaptersViewModel.SearchState.ByTags -> {
                    searchTypeSpinner.setSelection(1)
                    titleTextView.text = getString(R.string.chapters_title)
                    searchEditText.isVisible = false
                    clearSearchButton.isVisible = false
                    tagsRecyclerView.isVisible = true
                    tagsRecyclerView.requireBindingRecyclerViewAdapter().loadItems(listOf(SelectTagsButtonItem) + searchState.tags)
                    searchEditText.hideKeyboard()
                }
                is ChaptersViewModel.SearchState.OnlyFavorites -> {
                    searchTypeSpinner.setSelection(3)
                    titleTextView.text = getString(R.string.chapters_title)
                    searchEditText.isVisible = false
                    clearSearchButton.isVisible = false
                    tagsRecyclerView.isVisible = false
                    searchEditText.hideKeyboard()
                }
            }
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.items }.onEachChanged {
            chaptersRecyclerView.requireBindingRecyclerViewAdapter().loadItems(it ?: emptyList())
            noChaptersTextView.isVisible = it != null && it.isEmpty()
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.canNavigateBack ?: false }.onEachChanged {
            if (it) {
                backButton.setImageResource(R.drawable.ic_back)
                backButton.setPadding(4.dp)
                backButton.updateLayoutParams {
                    width = 32.dp
                    height = 32.dp
                }
            } else {
                backButton.setImageResource(R.drawable.main_screen_app_icon)
                backButton.setPadding(0)
                backButton.updateLayoutParams {
                    width = 32.dp
                    height = 32.dp
                }
            }

            backPressedCallback.isEnabled = it
        }.launchWith(viewLifecycleOwner)
    }

    private fun BindingRecyclerViewAdapterBuilder.setupChapterItem() =
        setup<ChaptersViewModel.Item.Chapter, ChapterItemBinding>(ChapterItemBinding::inflate) {
            bind { scope, item ->
                nameTextView.text = item.name

                chapterIconsRepository.chapterIconFlow(item.id).onEachChanged {
                    iconImageView.isVisible = it != null
                    if (it != null) {
                        Glide.with(root.context)
                            .load("file:///android_asset/${it.source}")
                            .into(iconImageView)
                    }
                }.launchIn(scope)

                if (item.isBlocked) {
                    root.children.forEach { it.isEnabled = false }
                    favoriteImageView.setImageResource(R.drawable.ic_lock)
                } else {
                    root.children.forEach { it.isEnabled = true }
                    favoriteImageView.setImageResource(
                        if (item.isFavorite) R.drawable.ic_star_checked
                        else R.drawable.ic_star_unchecked
                    )
                }

                favoriteImageView.setOnClickListener {
                    viewModel.setChapterFavorite(item.id, !item.isFavorite)
                }

                root.setOnClickListener {
                    delegate.navigateToChapter(this@ChaptersFragment, item.id)
                }

                favoriteImageView.setOnClickListener {
                    viewModel.setChapterFavorite(item.id, !item.isFavorite)
                }
            }

            diffUtil {
                areItemsTheSame { oldItem, newItem -> oldItem.id == newItem.id }
                areContentsTheSame { oldItem, newItem ->
                    oldItem.name == newItem.name
                            && oldItem.isFavorite == newItem.isFavorite
                            && oldItem.isBlocked == newItem.isBlocked
                }
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupGroupItem() =
        setup<ChaptersViewModel.Item.Group, GroupItemBinding>(GroupItemBinding::inflate) {
            bind { scope, item ->
                nameTextView.text = item.name
                chapterGroupIconsRepository.chapterGroupIconFlow(item.id).onEachChanged {
                    iconImageView.isVisible = it != null
                    if (it != null) {
                        Glide.with(root.context)
                            .load("file:///android_asset/${it.source}")
                            .into(iconImageView)
                    }
                }.launchIn(scope)
                root.setOnClickListener {
                    viewModel.navigateInGroup(item.id)
                }
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupFavoriteGroupItem() =
        setup<ChaptersViewModel.Item.FavoritesGroup, TitleItemBinding>(TitleItemBinding::inflate) {
            bind { _ ->
                textView.setText(R.string.chapters_favoriteGroup_title)
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupAdItem() =
        setup<ChaptersViewModel.Item.NativeAd, NativeAdItemBinding>(NativeAdItemBinding::inflate) {
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
                    delegate.navigateToPremiumOffer(this@ChaptersFragment)
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

    private inner class SearchTypeSpinnerAdapter(
        context: Context,
        list: List<String>
    ) : ArrayAdapter<String>(context, R.layout.spinner_item, list) {

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ) = if (convertView == null) {
            SpinnerItemBinding.inflate(layoutInflater, parent, false)
        } else {
            SpinnerItemBinding.bind(convertView)
        }.apply {
            val item = getItem(position) ?: return@apply
            titleTextView.text = item
        }.root

        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ) = getView(position, convertView, parent)
    }

    private inner class TagSelectionBottomSheetDialogFragmentDelegate : TagSelectionBottomSheetDialogFragment.Delegate {
        override fun onSelected(fragment: TagSelectionBottomSheetDialogFragment, tagIds: List<Int>) {
            fragment.dismiss()
            viewModel.setSelectedTagIds(tagIds)
        }
    }

    object SelectTagsButtonItem

    interface Delegate {
        fun navigateToChapter(fragment: ChaptersFragment, chapterId: Int)
        fun navigateToSettings(fragment: ChaptersFragment)
        fun navigateToPremiumOffer(fragment: ChaptersFragment)
    }
}