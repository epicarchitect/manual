package manual.app

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import manual.app.ads.InterstitialAdManager
import manual.app.ads.NativeAdsManager
import manual.app.ads.RewardedAdManager
import manual.app.database.IdGenerator
import manual.app.database.MainDatabase
import manual.app.parser.ChapterParser
import manual.app.premium.BillingClientManager
import manual.app.premium.PremiumManager
import manual.app.repository.AppBackgroundsRepository
import manual.app.repository.ChapterGroupIconsRepository
import manual.app.repository.ChapterGroupsRepository
import manual.app.repository.ChapterIconsRepository
import manual.app.repository.ChapterTagsRepository
import manual.app.repository.ChaptersRepository
import manual.app.repository.FavoriteChapterIdsRepository
import manual.app.repository.LaunchConfigRepository
import manual.app.repository.MonetizationConfigRepository
import manual.app.repository.NotesRepository
import manual.app.repository.TagGroupsRepository
import manual.app.repository.TagsRepository
import manual.app.repository.UnblockedChapterIdsRepository
import manual.app.ui.AlertDialogManager
import manual.app.ui.AudioAssetPlayer
import manual.app.ui.FontScaleManager
import manual.app.ui.NightModeManager
import manual.app.viewmodel.ChapterViewModel
import manual.app.viewmodel.ChaptersViewModel
import manual.app.viewmodel.NoteViewModel
import manual.app.viewmodel.NotesViewModel
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
        MobileAds.initialize(this)
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@App)
            modules(singlesModule(), viewModelsModule())
        }
    }

    fun singlesModule() = module(createdAtStart = true) {
        single { IdGenerator(this@App) }
        single { AlertDialogManager() }
        single { RewardedAdManager(this@App) }
        single { InterstitialAdManager(this@App) }
        single { NativeAdsManager(this@App) }
        single { ReviewManagerFactory.create(this@App) }
        single { AppUpdateManagerFactory.create(this@App) }
        single { AudioAssetPlayer(this@App, CoroutineScope(Dispatchers.IO)) }
        single { MainDatabase.create(this@App) }
        single { get<MainDatabase>().favoriteChapterIdsDao }
        single { get<MainDatabase>().unblockedChapterIdsDao }
        single { get<MainDatabase>().notesDao }
        single { assets }
        single { GsonBuilder().create() }
        single { FavoriteChapterIdsRepository(get()) }
        single { NotesRepository(get(), get(), get(), get()) }
        single { UnblockedChapterIdsRepository(get()) }
        single { ChapterGroupsRepository(get(), get()) }
        single { ChapterParser() }
        single { ChaptersRepository(get(), get()) }
        single { TagGroupsRepository(get(), get()) }
        single { TagsRepository(get(), get()) }
        single { MonetizationConfigRepository(get(), get()) }
        single { LaunchConfigRepository(get(), get()) }
        single { ChapterIconsRepository(get(), get()) }
        single { ChapterGroupIconsRepository(get(), get()) }
        single { ChapterTagsRepository(get(), get()) }
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
        viewModel {
            ChaptersViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        viewModel { (chapterId: Int) ->
            ChapterViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                chapterId
            )
        }
        viewModel { (selectedTagIds: List<Int>) ->
            TagSelectionViewModel(
                get(),
                get(),
                selectedTagIds
            )
        }
        viewModel { NotesViewModel(get()) }
        viewModel { (noteId: Int?) -> NoteViewModel(get(), noteId) }
    }
}