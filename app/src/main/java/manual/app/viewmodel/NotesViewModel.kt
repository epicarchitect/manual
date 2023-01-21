package manual.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import manual.app.repository.NotesRepository

class NotesViewModel(
    notesRepository: NotesRepository
) : ViewModel() {

    val notes = notesRepository.notesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

}