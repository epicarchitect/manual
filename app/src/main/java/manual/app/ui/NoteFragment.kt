package manual.app.ui

import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import manual.app.R
import manual.app.databinding.NoteFragmentBinding
import manual.app.viewmodel.NoteViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.CoreFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NoteFragment(
    private val delegate: Delegate
) : CoreFragment<NoteFragmentBinding>(NoteFragmentBinding::inflate) {

    private val viewModel: NoteViewModel by viewModel {
        parametersOf(arguments?.getInt(Argument.Int.NOTE_ID))
    }

    override fun NoteFragmentBinding.onCreated() {
        titleEditText.doAfterTextChanged {
            viewModel.title.value = it?.toString() ?: ""
        }

        contentEditText.doAfterTextChanged {
            viewModel.content.value = it?.toString() ?: ""
        }

        saveButton.setOnClickListener {
            viewModel.save()
            requireActivity().onBackPressed()
            Toast.makeText(requireContext(), R.string.note_saved_message, Toast.LENGTH_SHORT).show()
        }

        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        deleteButton.setOnClickListener {
            viewModel.delete()
            delegate.onNoteDelete()
        }

        viewModel.title.onEachChanged {
            if (titleEditText.text?.toString() != it) {
                titleEditText.setText(it)
            }
        }.launchWith(viewLifecycleOwner)

        viewModel.content.onEachChanged {
            if (contentEditText.text?.toString() != it) {
                contentEditText.setText(it)
            }
        }.launchWith(viewLifecycleOwner)
    }

    object Argument {
        object Int {
            const val NOTE_ID = "noteId"
        }
    }

    companion object {
        fun buildArguments(noteId: Int?) = bundleOf(
            Argument.Int.NOTE_ID to noteId
        )
    }

    interface Delegate {
        fun onNoteDelete()
    }
}