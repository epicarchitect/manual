package manual.app.repository

import android.content.res.AssetManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import manual.app.data.AppBackground
import manual.core.resources.mapEachFile

class AppBackgroundsRepository(
    private val dataStore: DataStore<Preferences>,
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val appBackgroundsStateFlow = MutableStateFlow<List<AppBackground>?>(null)

    init {
        coroutineScope.launch {
            appBackgroundsStateFlow.value = assetManager.mapEachFile("backgrounds") {
                gson.fromJson(it, AppBackground::class.java)
            }.toMutableList().apply {
                add(0, lightAppBackground)
                add(1, nightAppBackground)
            }
        }
    }

    fun appBackgroundsFlow() = appBackgroundsStateFlow.filterNotNull()

    fun currentAppBackgroundFlow() = combine(
        appBackgroundsFlow(),
        dataStore.data.map { it[DataStoreKey.currentAppBackgroundId] ?: lightAppBackground.id }
    ) { appBackgrounds, currentId ->
        appBackgrounds.first { it.id == currentId }
    }

    fun setCurrentAppBackground(id: Int) = coroutineScope.launch {
        dataStore.edit { it[DataStoreKey.currentAppBackgroundId] = id }
    }

    private object DataStoreKey {
        val currentAppBackgroundId = intPreferencesKey("backgroundId")
    }

    companion object {
        val lightAppBackground = AppBackground(
            -1,
            false,
            ""
        )

        val nightAppBackground = AppBackground(
            -2,
            true,
            ""
        )
    }
}