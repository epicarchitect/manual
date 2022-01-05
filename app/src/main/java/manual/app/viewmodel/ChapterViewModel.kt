@file:Suppress("UNCHECKED_CAST")

package manual.app.viewmodel

import android.content.res.AssetManager
import manual.core.viewmodel.CoreViewModel
import android.net.Uri
import android.text.Spanned
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.delay
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

    init {
        combine(
            tagsRepository.tagsFlow(),
            chaptersRepository.chapterFlow(chapterId),
            favoriteChapterIdsRepository.isFavoriteChapterFlow(chapterId),
            monetizationConfigRepository.monetizationConfigFlow(),
            premiumManager.premiumEnabledFlow().filterNotNull(),
            contentsRepository.contentsFlow()
        ) {
            val tagDatas = it[0] as List<TagData>
            val chapterData = it[1] as ChapterData
            val isFavorite = it[2] as Boolean
            val monetizationConfig = it[3] as MonetizationConfig
            val premiumEnabled = it[4] as Boolean
            val contentDatas = it[5] as List<ContentData>

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
                            if (it.source.split(".").last() == "html") {
                                Content.Html(
                                    contentId = it.id,
                                    name = it.name,
                                    html = HtmlCompat.fromHtml(assetManager.read(it.source), HtmlCompat.FROM_HTML_MODE_COMPACT),
                                )
                            } else {
                                Content.Image(
                                    contentId = it.id,
                                    name = it.name,
                                    uri = Uri.parse("file:///android_asset/${it.source}")
                                )
                            }
                        }
                    }
                )
            }
        }.launch()
    }

    fun setFavorite(isFavorite: Boolean) {
        favoriteChapterIdsRepository.setFavoriteChapterId(chapterId, isFavorite)
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