package manual.app.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import manual.app.data.Note
import manual.app.data.NotesConfig
import manual.app.database.IdGenerator
import manual.app.database.NoteEntity
import manual.app.database.NotesDao
import manual.core.coroutines.flow.mapItems
import manual.core.resources.read

class NotesRepository(
    private val notesDao: NotesDao,
    private val idGenerator: IdGenerator,
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val configState = MutableStateFlow<NotesConfig?>(null)

    fun configFlow() = configState.filterNotNull()

    init {
        coroutineScope.launch {
            configState.value = gson.fromJson(
                assetManager.read("notes/config.json"),
                NotesConfig::class.java
            )
        }
    }

    suspend fun save(note: Note) = withContext(Dispatchers.IO) {
        if (note.id == 0) {
            val newId = idGenerator.nextId().toInt()
            notesDao.save(note.copy(id = newId).toEntity())
            return@withContext newId
        } else {
            notesDao.save(note.toEntity())
            return@withContext note.id
        }
    }

    fun notesFlow() = notesDao.entitiesFlow().mapItems { it.toNote() }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        notesDao.delete(id)
    }

    suspend fun getNote(id: Int) = withContext(Dispatchers.IO) {
        notesDao.entityFlow(id).firstOrNull()?.toNote()
    }

    private fun NoteEntity.toNote() = Note(
        id, title, content
    )

    private fun Note.toEntity() = NoteEntity(
        id, title, content
    )
}