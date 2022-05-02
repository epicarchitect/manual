package manual.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.Glide
import kolmachikhin.alexander.binding.recyclerview.adapter.BindingRecyclerViewAdapter
import kolmachikhin.alexander.binding.recyclerview.adapter.BindingRecyclerViewAdapterBuilder
import kolmachikhin.alexander.binding.recyclerview.adapter.requireBindingRecyclerViewAdapter
import kotlinx.coroutines.flow.filterNotNull
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

        chaptersRecyclerView.adapter = BindingRecyclerViewAdapter {
            setupChapterItem()
            setupGroupItem()
            setupFavoriteGroupItem()
            setupAdItem()
        }

        viewModel.state.map { it?.availableSearchTypes }.filterNotNull().onEachChanged { searchTypes ->
            searchTypeLayout.isVisible = searchTypes.isNotEmpty()
            bottomDivider.isVisible = searchTypes.isNotEmpty()
            searchTypeSpinner.isEnabled = searchTypes.size > 1

            searchTypeSpinner.adapter = SearchTypeSpinnerAdapter(
                requireContext(),
                searchTypes.map {
                    when (it) {
                        ChaptersViewModel.SearchState.ByGroups::class -> getString(R.string.chapters_searchByGroups)
                        ChaptersViewModel.SearchState.ByTags::class -> getString(R.string.chapters_searchByTags)
                        ChaptersViewModel.SearchState.ByName::class -> getString(R.string.chapters_searchByName)
                        ChaptersViewModel.SearchState.ByFavorites::class -> getString(R.string.chapters_searchByFavorites)
                        else -> error("Unexpected search type: ${it::class}")
                    }
                }
            )

            searchTypeSpinner.setSelection(searchTypes.indexOf(viewModel.state.value!!.searchState::class))

            searchTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.setSearchType(searchTypes[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }.launchWith(viewLifecycleOwner)

        tagsRecyclerView.adapter = BindingRecyclerViewAdapter {
            setup<ChaptersViewModel.Tag, TagItemBinding>(TagItemBinding::inflate) {
                bind { item ->
                    chip.text = item.name
                }
            }

            setup<SelectTagsButtonItem, SelectTagsButtonItemBinding>(SelectTagsButtonItemBinding::inflate) {
                bind { _ ->
                    root.setOnClickListener {
                        fragmentFactoryStore.instantiate<TagSelectionBottomSheetDialogFragment>().apply {
                            arguments = TagSelectionBottomSheetDialogFragment.buildArguments(
                                (viewModel.state.value!!.searchState as ChaptersViewModel.SearchState.ByTags).tags.map { it.id }
                            )
                        }.show(childFragmentManager, null)
                    }
                }
            }

            setup<NoTagsSelectedItem, NoTagsSelectedItemBinding>(NoTagsSelectedItemBinding::inflate)
        }

        viewModel.state.map { it == null }.onEachChanged {
            progressBar.isVisible = it
        }.launchWith(viewLifecycleOwner)

        val backPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, false) {
            viewModel.navigateBack()
        }

        searchTypePremiumOfferLearnMoreButton.setOnClickListener {
            delegate.navigateToPremiumOffer(this@ChaptersFragment)
        }

        backButton.setOnClickListener {
            viewModel.navigateBack()
        }

        viewModel.state.map { it?.searchState }.filterNotNull().onEachChanged { searchState ->
            searchTypeSpinner.setSelection(viewModel.state.value!!.availableSearchTypes.indexOf(searchState::class))

            when (searchState) {
                is ChaptersViewModel.SearchState.ByGroups -> {
                    searchEditText.isVisible = false
                    clearSearchButton.isVisible = false
                    tagsRecyclerView.isVisible = false
                    searchTypePremiumOfferLayout.isVisible = false

                    if (searchState.isRoot) {
                        titleTextView.text = getString(R.string.chapters_title)
                    } else {
                        titleTextView.text = searchState.currentGroupTitle
                    }
                    searchEditText.hideKeyboard()
                }
                is ChaptersViewModel.SearchState.ByName -> {
                    titleTextView.text = getString(R.string.chapters_title)
                    searchEditText.isVisible = true
                    clearSearchButton.isVisible = true
                    tagsRecyclerView.isVisible = false
                    searchTypePremiumOfferLayout.isVisible = false

                    if (searchEditText.text.toString() != searchState.name) {
                        searchEditText.setText(searchState.name)
                    }

                    clearSearchButton.isVisible = !searchState.name.isNullOrEmpty()
                }
                is ChaptersViewModel.SearchState.ByTags -> {
                    titleTextView.text = getString(R.string.chapters_title)
                    clearSearchButton.isVisible = false
                    searchEditText.isVisible = false

                    if (searchState.isBlocked) {
                        searchTypePremiumOfferLayout.isVisible = true
                        tagsRecyclerView.isVisible = false
                    } else {
                        searchTypePremiumOfferLayout.isVisible = false
                        tagsRecyclerView.isVisible = true
                        tagsRecyclerView.requireBindingRecyclerViewAdapter().loadItems(
                            listOf(SelectTagsButtonItem)
                                    + searchState.tags
                                    + if (searchState.tags.isEmpty()) listOf(NoTagsSelectedItem) else listOf()
                        )
                    }
                    searchEditText.hideKeyboard()
                }
                is ChaptersViewModel.SearchState.ByFavorites -> {
                    titleTextView.text = getString(R.string.chapters_title)
                    searchEditText.isVisible = false
                    clearSearchButton.isVisible = false
                    tagsRecyclerView.isVisible = false
                    searchTypePremiumOfferLayout.isVisible = false
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
                    if (it == null) return@onEachChanged

                    val parts = it.source.split("/")
                    when (parts.first()) {
                        "drawable" -> {
                            iconImageView.setImageResource(
                                root.context.resources.getIdentifier(parts.last(), "drawable", root.context.packageName)
                            )
                        }
                        else -> {
                            Glide.with(root.context)
                                .load("file:///android_asset/${it.source}")
                                .into(iconImageView)
                        }
                    }
                }.launchIn(scope)

                if (item.isBlocked) {
                    if (item.canUnblockByAd) {
                        root.children.forEach { it.isEnabled = true }
                        favoriteImageView.isEnabled = false
                        favoriteImageView.setImageResource(R.drawable.ic_unblock_key)
                    } else {
                        root.children.forEach { it.isEnabled = false }
                        favoriteImageView.setImageResource(R.drawable.ic_lock)
                    }
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
                            && oldItem.canUnblockByAd == newItem.canUnblockByAd
                }
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupGroupItem() =
        setup<ChaptersViewModel.Item.Group, GroupItemBinding>(GroupItemBinding::inflate) {
            bind { scope, item ->
                nameTextView.text = item.name
                chapterGroupIconsRepository.chapterGroupIconFlow(item.id).onEachChanged {
                    iconImageView.isVisible = it != null
                    if (it == null) return@onEachChanged

                    val parts = it.source.split("/")
                    when (parts.first()) {
                        "drawable" -> {
                            iconImageView.setImageResource(
                                root.context.resources.getIdentifier(parts.last(), "drawable", root.context.packageName)
                            )
                        }
                        else -> {
                            Glide.with(root.context)
                                .load("file:///android_asset/${it.source}")
                                .into(iconImageView)
                        }
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

    object NoTagsSelectedItem

    interface Delegate {
        fun navigateToChapter(fragment: ChaptersFragment, chapterId: Int)
        fun navigateToSettings(fragment: ChaptersFragment)
        fun navigateToPremiumOffer(fragment: ChaptersFragment)
    }
}