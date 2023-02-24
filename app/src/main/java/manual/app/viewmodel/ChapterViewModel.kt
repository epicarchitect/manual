@file:Suppress("UNCHECKED_CAST")

package manual.app.viewmodel

import android.net.Uri
import android.text.Spanned
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import manual.app.data.ChapterTags
import manual.app.data.MonetizationConfig
import manual.app.premium.PremiumManager
import manual.app.repository.*
import manual.core.viewmodel.CoreViewModel
import manual.app.data.Chapter as ChapterData
import manual.app.data.Content as ContentData
import manual.app.data.Tag as TagData

class ChapterViewModel(
    tagsRepository: TagsRepository,
    monetizationConfigRepository: MonetizationConfigRepository,
    premiumManager: PremiumManager,
    chaptersRepository: ChaptersRepository,
    chapterTagsRepository: ChapterTagsRepository,
    private val unblockedChapterIdsRepository: UnblockedChapterIdsRepository,
    private val favoriteChapterIdsRepository: FavoriteChapterIdsRepository,
    private val chapterId: Int
) : CoreViewModel<ChapterViewModel.State>() {

    init {
        combine(
            tagsRepository.tagsFlow(),
            chaptersRepository.chapterFlow(chapterId),
            favoriteChapterIdsRepository.isFavoriteChapterFlow(chapterId),
            unblockedChapterIdsRepository.isUnblockedChapterFlow(chapterId),
            monetizationConfigRepository.monetizationConfigFlow(),
            premiumManager.premiumEnabledFlow().filterNotNull(),
            chapterTagsRepository.chapterTagsFlow(chapterId)
        ) {
            val tagDatas = it[0] as List<TagData>
            val chapterData = it[1] as ChapterData
            val isFavorite = it[2] as Boolean
            val isUnblocked = it[3] as Boolean
            val monetizationConfig = it[4] as MonetizationConfig
            val premiumEnabled = it[5] as Boolean
            val chapterTags = it[6] as? ChapterTags

            updateState {
                State(
                    title = chapterData.name,
                    tags = if (chapterTags == null) {
                        emptyList()
                    } else {
                        tagDatas.filter {
                            chapterTags.tagIds.contains(it.id)
                        }.map {
                            Tag(it.name)
                        }
                    },
                    isFavorite = isFavorite,
                    isBlocked = monetizationConfig.restrictChapters
                            && !isUnblocked
                            && !premiumEnabled
                            && !monetizationConfig.availableChapterIds.contains(chapterData.id),
                    canUnblockByAd = monetizationConfig.restrictChapters
                            && monetizationConfig.unblockChaptersForRewardedAd
                            && monetizationConfig.rewardedChapterIds.contains(chapterId)
                            && !monetizationConfig.availableChapterIds.contains(chapterId)
                            && !premiumEnabled,
                    contents = chapterData.contents.map {
                        when (it) {
                            is ContentData.Image -> Content.Image(
                                0,
                                it.name ?: "",
                                Uri.parse("file:///android_asset/${it.source}")
                            )
                            is ContentData.Gif -> Content.Image(
                                0,
                                it.name ?: "",
                                Uri.parse("file:///android_asset/${it.source}")
                            )
                            is ContentData.Audio -> Content.Audio(0, it.name ?: "", it.source)
                            is ContentData.Text -> Content.Html(
                                0,
                                "",
                                HtmlCompat.fromHtml(it.value, HtmlCompat.FROM_HTML_MODE_COMPACT)
                            )
                            is ContentData.Video -> Content.Video(0, it.name ?: "", it.source)
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

    fun unblock() {
        unblockedChapterIdsRepository.setUnblockedChapterId(chapterId, true)
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

        data class Audio(
            val contentId: Int,
            val name: String,
            val source: String
        ) : Content()

        data class Video(
            val contentId: Int,
            val name: String,
            val source: String
        ) : Content()

        class NativeAd : Content()
    }

    data class Tag(val name: String)

    data class State(
        val title: String,
        val tags: List<Tag>,
        val isFavorite: Boolean,
        val isBlocked: Boolean,
        val canUnblockByAd: Boolean,
        val contents: List<Content>
    )
}