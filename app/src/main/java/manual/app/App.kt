package manual.app

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import manual.app.database.MainDatabase
import manual.app.premium.BillingClientManager
import manual.app.premium.PremiumManager
import manual.app.repository.*
import manual.app.ui.FontScaleManager
import manual.app.ui.NightModeManager
import manual.app.viewmodel.ChapterViewModel
import manual.app.viewmodel.ChaptersViewModel
import manual.app.viewmodel.TagSelectionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@App)
            modules(singlesModule(), viewModelsModule())
        }
    }

    fun singlesModule() = module(createdAtStart = true) {
        single { ReviewManagerFactory.create(this@App) }
        single { AppUpdateManagerFactory.create(this@App) }
        single { Room.databaseBuilder(this@App, MainDatabase::class.java, "main").build() }
        single { get<MainDatabase>().favoriteChapterIdsDao }
        single { get<MainDatabase>().unblockedContentIdsDao }
        single { assets }
        single { GsonBuilder().create() }
        single { FavoriteChapterIdsRepository(get()) }
        single { UnblockedContentIdsRepository(get()) }
        single { ContentsRepository(get(), get()) }
        single { ChapterGroupsRepository(get(), get()) }
        single { ChaptersRepository(get(), get()) }
        single { TagGroupsRepository(get(), get()) }
        single { TagsRepository(get(), get()) }
        single { MonetizationConfigRepository(get(), get()) }
        single { PremiumManager(this@App, get()) }
        single { BillingClientManager(this@App, 3000) }
        single {
            AppBackgroundsRepository(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = null,
                    migrations = emptyList(),
                    scope = CoroutineScope(Dispatchers.Default)
                ) { preferencesDataStoreFile("AppBackgrounds") },
                get(),
                get()
            )
        }
        single { NightModeManager(getSharedPreferences("NightModeManager", MODE_PRIVATE)) }
        single { FontScaleManager(getSharedPreferences("FontScaleManager", MODE_PRIVATE)) }
    }

    fun viewModelsModule() = module {
        viewModel { ChaptersViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { (chapterId: Int) -> ChapterViewModel(get(), get(), get(), get(), get(), get(), get(), get(), chapterId) }
        viewModel { (selectedTagIds: List<Int>) -> TagSelectionViewModel(get(), get(), selectedTagIds) }
    }
}