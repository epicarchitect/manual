package manual.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import manual.app.data.Note
import manual.app.repository.NotesRepository

class NoteViewModel(
    private val notesRepository: NotesRepository,
    private val noteId: Int?
) : ViewModel() {

    private var currentId = noteId ?: 0

    val title = MutableStateFlow("")
    val content = MutableStateFlow("")

    init {
        if (noteId != null) {
            viewModelScope.launch {
                val note = notesRepository.getNote(noteId) ?: return@launch
                title.value = note.title
                content.value = note.content
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            if (noteId == null) {
                currentId = notesRepository.save(
                    Note(
                        0,
                        title.value,
                        content.value
                    )
                )
            } else {
                notesRepository.save(
                    Note(
                        noteId,
                        title.value,
                        content.value
                    )
                )
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            notesRepository.delete(currentId)
        }
    }
}