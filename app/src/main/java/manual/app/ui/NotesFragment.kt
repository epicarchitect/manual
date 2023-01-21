package manual.app.ui

import androidx.core.view.isVisible
import epicarchitect.recyclerview.EpicAdapter
import epicarchitect.recyclerview.bind
import epicarchitect.recyclerview.requireEpicAdapter
import manual.app.data.Note
import manual.app.databinding.NoteItemBinding
import manual.app.databinding.NotesFragmentBinding
import manual.app.viewmodel.NotesViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.CoreFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotesFragment(
    private val delegate: Delegate
) : CoreFragment<NotesFragmentBinding>(NotesFragmentBinding::inflate) {

    private val viewModel: NotesViewModel by viewModel()

    override fun NotesFragmentBinding.onCreated() {
        notesRecyclerView.adapter = EpicAdapter {
            setup<Note, NoteItemBinding>(NoteItemBinding::inflate) {
                bind { item ->
                    titleTextView.text = item.title
                    contentTextView.text = item.content
                    root.setOnClickListener {
                        delegate.onNoteCLick(item)
                    }
                }
            }
        }

        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        addButton.setOnClickListener {
            delegate.onAddNoteCLick()
        }

        viewModel.notes.onEachChanged {
            notesRecyclerView.requireEpicAdapter().loadItems(it)
            noNotesTextView.isVisible = it.isEmpty()
        }.launchWith(viewLifecycleOwner)
    }

    interface Delegate {
        fun onNoteCLick(note: Note)
        fun onAddNoteCLick()
    }
}