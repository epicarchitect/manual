package manual.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.mctech.library.keyboard.visibilitymonitor.KeyboardVisibilityMonitor
import kotlinx.coroutines.flow.map
import manual.app.R
import manual.core.coroutines.flow.launchWith
import manual.app.viewmodel.ChaptersViewModel
import manual.core.fragment.CoreFragment
import manual.core.fragment.FragmentFactoryStore
import manual.core.fragment.setFactory
import manual.core.view.*
import manual.app.data.Tag
import manual.app.databinding.*
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.instantiate
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

class ChaptersFragment(private val delegate: Delegate) : CoreFragment<ChaptersFragmentBinding>(ChaptersFragmentBinding::inflate) {

    private val viewModel: ChaptersViewModel by sharedViewModel()
    private var searchTextChangeListener: TextWatcher? = null

    override fun FragmentFactoryStore.setup() {
        setFactory { TagSelectionBottomSheetDialogFragment(TagSelectionBottomSheetDialogFragmentDelegate()) }
    }

    @SuppressLint("SetTextI18n")
    override fun ChaptersFragmentBinding.onCreated() {
        moreButton.setOnClickListener {
            it.showPopupMenu(R.menu.main) {
                when (it.itemId) {
                    R.id.settings -> delegate.navigateToSettings(this@ChaptersFragment)
                    R.id.review -> goToAppInStore()
                }
            }
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

        chaptersRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setupChapterItem()
            setupGroupItem()
            setupFavoriteGroupItem()
            setupChestItem()
        }

        tagsRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setup<ChaptersViewModel.Tag, TagItemBinding>(TagItemBinding::inflate) {
                bind { item ->
                    chip.text = item.name
                }
            }
        }

        viewModel.state.map { it == null }.onEachChanged {
            titleTextView.alpha = if (it) 0.0f else 1.0f // для того чтоб небыло конфликта с searchText
            progressBar.isVisible = it
            searchButton.isVisible = !it
            moreButton.isVisible = !it
            divider.isVisible = !it
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.selectedTags }.onEachChanged { selectedTags ->
            if (selectedTags == null) {
                tagsButton.isVisible = false
                tagsEmptyTextView.isVisible = false
                tagsButton.setOnClickListener(null)
            } else {
                tagsButton.isVisible = true
                tagsEmptyTextView.isVisible = selectedTags.isEmpty()
                if (selectedTags.isEmpty()) {
                    tagsButton.text = getString(R.string.chapters_tags_button)
                } else {
                    tagsButton.text = getString(R.string.chapters_tagsCount_button, selectedTags.size)
                }

                tagsButton.setOnClickListener {
                    fragmentFactoryStore.instantiate<TagSelectionBottomSheetDialogFragment>().apply {
                        arguments = TagSelectionBottomSheetDialogFragment.buildArguments(selectedTags.map { it.id })
                    }.show(childFragmentManager, null)
                }
            }

            tagsRecyclerView.requireBindingRecyclerViewAdapter().loadItems(selectedTags ?: emptyList())
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.searchText }.onEachChanged {
            if (it == null) {
                titleTextView.isVisible = true
                searchEditText.isVisible = false
                searchButton.setImageResource(R.drawable.ic_search)
                searchButton.setOnClickListener {
                    searchTextChangeListener = searchEditText.doAfterTextChanged { viewModel.setSearchText(it?.toString() ?: "") }
                    viewModel.setSearchText("")
                    searchEditText.showKeyboard()
                }
            } else {
                titleTextView.isVisible = false
                searchEditText.isVisible = true
                searchButton.setImageResource(R.drawable.ic_clear)
                searchButton.setOnClickListener {
                    searchEditText.removeTextChangedListener(searchTextChangeListener)
                    searchEditText.hideKeyboard()
                    viewModel.setSearchText(null)
                }
            }

            if (searchEditText.text?.toString() != it) {
                searchEditText.setText(it)
            }
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.items }.onEachChanged {
            chaptersRecyclerView.requireBindingRecyclerViewAdapter().loadItems(it ?: emptyList())
            noChaptersTextView.isVisible = it != null && it.isEmpty()
        }.launchWith(viewLifecycleOwner)
    }

    private fun BindingRecyclerViewAdapterBuilder.setupChapterItem() =
        setup<ChaptersViewModel.Item.Chapter, ChapterItemBinding>(ChapterItemBinding::inflate) {
            bind { item ->
                nameTextView.text = item.name

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
        setup<ChaptersViewModel.Item.Group, TitleItemBinding>(TitleItemBinding::inflate) {
            bind { item ->
                textView.text = item.name
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupFavoriteGroupItem() =
        setup<ChaptersViewModel.Item.FavoritesGroup, TitleItemBinding>(TitleItemBinding::inflate) {
            bind { _ ->
                textView.setText(R.string.chapters_favoriteGroup_title)
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupChestItem() =
        setup<ChaptersViewModel.Item.Chest, ChestItemBinding>(ChestItemBinding::inflate) {
            bind { _ ->
                root.setOnClickListener {
                    delegate.navigateToChapter(this@ChaptersFragment, -1)
                }
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

    private inner class TagSelectionBottomSheetDialogFragmentDelegate : TagSelectionBottomSheetDialogFragment.Delegate {
        override fun onSelected(fragment: TagSelectionBottomSheetDialogFragment, tagIds: List<Int>) {
            viewModel.setSelectedTagIds(tagIds)
            fragment.dismiss()
        }
    }

    interface Delegate {
        fun navigateToChapter(fragment: ChaptersFragment, chapterId: Int)
        fun navigateToSettings(fragment: ChaptersFragment)
    }
}