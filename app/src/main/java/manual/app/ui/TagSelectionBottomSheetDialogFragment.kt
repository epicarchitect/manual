package manual.app.ui

import android.annotation.SuppressLint
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import epicarchitect.recyclerview.EpicAdapter
import epicarchitect.recyclerview.bind
import epicarchitect.recyclerview.requireEpicAdapter
import kotlinx.coroutines.flow.map
import manual.app.databinding.TagSelectionBottomSheetDialogFragmentBinding
import manual.app.databinding.TagSelectionItemBinding
import manual.app.databinding.TitleItemBinding
import manual.app.viewmodel.TagSelectionViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.dialog.bottomsheet.CoreBottomSheetDialogFragment
import manual.core.os.require
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class TagSelectionBottomSheetDialogFragment(
    private val delegate: Delegate
) : CoreBottomSheetDialogFragment<TagSelectionBottomSheetDialogFragmentBinding>(
    TagSelectionBottomSheetDialogFragmentBinding::inflate,
    fullscreen = true
) {

    private val viewModel: TagSelectionViewModel by viewModel {
        parametersOf(arguments.require<IntArray>(Argument.IntArray.SELECTED_TAG_IDS).toList())
    }

    @SuppressLint("SetTextI18n")
    override fun TagSelectionBottomSheetDialogFragmentBinding.onCreated() {
        doneButton.setOnClickListener {
            delegate.onSelected(
                this@TagSelectionBottomSheetDialogFragment,
                viewModel.getSelectedTagIds()
            )
        }

        selectAllButton.setOnClickListener {
            viewModel.selectAll()
        }

        clearAllButton.setOnClickListener {
            viewModel.clearAll()
        }

        recyclerView.itemAnimator = null
        recyclerView.adapter = EpicAdapter {
            setup<TagSelectionViewModel.Item.Tag, TagSelectionItemBinding>(TagSelectionItemBinding::inflate) {
                bind { item ->
                    checkbox.text = item.name
                    checkbox.isChecked = item.isChecked
                    checkbox.setOnClickListener {
                        viewModel.setTagSelected(item.id, !item.isChecked)
                    }
                }
            }

            setup<TagSelectionViewModel.Item.Group, TitleItemBinding>(TitleItemBinding::inflate) {
                bind { item ->
                    textView.isVisible = item.name.isNotEmpty()
                    textView.text = item.name
                }
            }
        }

        viewModel.state.map { it != null }.onEachChanged {
            progressBar.isVisible = !it
            titleTextView.isVisible = it
            doneButton.isVisible = it
            selectAllButton.isVisible = it
            clearAllButton.isVisible = it
            divider.isVisible = it
            recyclerView.isVisible = it
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.items }.onEachChanged {
            recyclerView.requireEpicAdapter().loadItems(it ?: emptyList())
        }.launchWith(viewLifecycleOwner)
    }

    interface Delegate {
        fun onSelected(fragment: TagSelectionBottomSheetDialogFragment, tagIds: List<Int>)
    }

    object Argument {
        object IntArray {
            const val SELECTED_TAG_IDS = "selectedTagIds"
        }
    }

    companion object {
        fun buildArguments(selectedTagIds: List<Int>) = bundleOf(
            Argument.IntArray.SELECTED_TAG_IDS to selectedTagIds.toIntArray()
        )
    }
}