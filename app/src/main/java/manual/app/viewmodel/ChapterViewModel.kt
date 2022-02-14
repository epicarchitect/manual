@file:Suppress("UNCHECKED_CAST")

package manual.app.viewmodel

import android.content.res.AssetManager
import android.media.MediaPlayer
import manual.core.viewmodel.CoreViewModel
import android.net.Uri
import android.text.Spanned
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.flow.*
import manual.app.data.Chapter as ChapterData
import manual.app.data.Content as ContentData
import manual.app.data.MonetizationConfig
import manual.app.data.Tag as TagData
import manual.app.premium.PremiumManager
import manual.app.repository.*
import manual.core.resources.read

class ChapterViewModel(
    tagsRepository: TagsRepository,
    monetizationConfigRepository: MonetizationConfigRepository,
    premiumManager: PremiumManager,
    chaptersRepository: ChaptersRepository,
    contentsRepository: ContentsRepository,
    private val favoriteChapterIdsRepository: FavoriteChapterIdsRepository,
    private val assetManager: AssetManager,
    private val chapterId: Int
) : CoreViewModel<ChapterViewModel.State>() {

    private var mediaPlayer: MediaPlayer? = null
    private val audioPlayingContentIdStateFlow = MutableStateFlow<Int?>(null)

    init {
        combine(
            tagsRepository.tagsFlow(),
            chaptersRepository.chapterFlow(chapterId),
            favoriteChapterIdsRepository.isFavoriteChapterFlow(chapterId),
            monetizationConfigRepository.monetizationConfigFlow(),
            premiumManager.premiumEnabledFlow().filterNotNull(),
            contentsRepository.contentsFlow(),
            audioPlayingContentIdStateFlow
        ) {
            val tagDatas = it[0] as List<TagData>
            val chapterData = it[1] as ChapterData
            val isFavorite = it[2] as Boolean
            val monetizationConfig = it[3] as MonetizationConfig
            val premiumEnabled = it[4] as Boolean
            val contentDatas = it[5] as List<ContentData>
            val audioPlayingContentId = it[6] as Int?

            updateState {
                State(
                    title = chapterData.name,
                    tags = tagDatas.filter {
                        chapterData.tagIds.contains(it.id)
                    }.map {
                        Tag(it.name)
                    },
                    isFavorite = isFavorite,
                    isBlocked = monetizationConfig.restrictChapters
                            && !premiumEnabled
                            && !monetizationConfig.availableChapterIds.contains(chapterData.id),
                    contents = contentDatas.let { contents ->
                        chapterData.contentIds.map { id ->
                            checkNotNull(contents.find { it.id == id })
                        }.map {
                            when(it.source.split(".").last()) {
                                "html" -> {
                                    Content.Html(
                                        contentId = it.id,
                                        name = it.name,
                                        html = HtmlCompat.fromHtml(assetManager.read(it.source), HtmlCompat.FROM_HTML_MODE_COMPACT),
                                    )
                                }
                                "mp3" -> {
                                    Content.Audio(
                                        contentId = it.id,
                                        name = it.name,
                                        path = it.source,
                                        isPlaying = audioPlayingContentId == it.id
                                    )
                                }
                                else -> {
                                    Content.Image(
                                        contentId = it.id,
                                        name = it.name,
                                        uri = Uri.parse("file:///android_asset/${it.source}")
                                    )
                                }
                            }
                        }
                    }.toMutableList().apply {
                        if (!premiumEnabled && monetizationConfig.showNativeAds) {
                            add(Content.NativeAd())
                        }
                    }
                )
            }
        }.launch()
    }

    fun setFavorite(isFavorite: Boolean) {
        favoriteChapterIdsRepository.setFavoriteChapterId(chapterId, isFavorite)
    }

    fun playAudioItem(item: Content.Audio) {
        audioPlayingContentIdStateFlow.value = item.contentId
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().also {
            val fd = assetManager.openFd(item.path)
            it.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            fd.close()
            it.prepare()
            it.start()
        }
    }

    fun stopAudioItem() {
        audioPlayingContentIdStateFlow.value = null
        mediaPlayer?.let {
            it.stop()
            it.release()
            mediaPlayer = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.let {
            it.stop()
            it.release()
            mediaPlayer = null
        }
    }

    sealed class Content {
        data class Html(
            val contentId: Int,
            val name: String,
            val html: Spanned
        ) : Content()

        data class Image(
            val contentId: Int,
            val name: String,
            val uri: Uri
        ) : Content()

        data class Audio(
            val contentId: Int,
            val name: String,
            val path: String,
            val isPlaying: Boolean
        ) : Content()

        class NativeAd : Content()
    }

    data class Tag(val name: String)

    data class State(
        val title: String,
        val tags: List<Tag>,
        val isFavorite: Boolean,
        val isBlocked: Boolean,
        val contents: List<Content>
    )
}