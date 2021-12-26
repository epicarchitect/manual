@file:Suppress("UNCHECKED_CAST")

package manual.app.viewmodel

import manual.core.viewmodel.CoreViewModel
import kotlinx.coroutines.flow.*
import manual.app.data.Chapter as ChapterData
import manual.app.data.ChapterGroup as ChapterGroupData
import manual.app.data.MonetizationConfig
import manual.app.data.Tag as TagData
import manual.app.premium.PremiumManager
import manual.app.repository.*

class ChaptersViewModel(
    chaptersRepository: ChaptersRepository,
    premiumManager: PremiumManager,
    monetizationConfigRepository: MonetizationConfigRepository,
    chapterGroupsRepository: ChapterGroupsRepository,
    tagsRepository: TagsRepository,
    private val favoriteChapterIdsRepository: FavoriteChapterIdsRepository
) : CoreViewModel<ChaptersViewModel.State>() {

    private val searchTextStateFlow = MutableStateFlow<String?>(null)
    private val selectedTagIdsStateFlow = MutableStateFlow<List<Int>>(emptyList())

    init {
        combine(
            chaptersRepository.chaptersFlow(),
            chapterGroupsRepository.chapterGroupsFlow(),
            selectedTagIdsStateFlow,
            searchTextStateFlow,
            premiumManager.premiumEnabledFlow().filterNotNull(),
            monetizationConfigRepository.monetizationConfigFlow(),
            favoriteChapterIdsRepository.favoriteChapterIdsFlow(),
            tagsRepository.tagsFlow()
        ) {
            val chapters = it[0] as List<ChapterData>
            val chapterGroups = it[1] as List<ChapterGroupData>
            val selectedTagIds = it[2] as List<Int>
            val searchText = it[3] as String?
            val premiumEnabled = it[4] as Boolean
            val monetizationConfig = it[5] as MonetizationConfig
            val favoriteChapterIds = it[6] as List<Int>
            val tags = it[7] as List<TagData>

            fun ChapterData.toItem() = Item.Chapter(
                id = id,
                name = name,
                isFavorite = favoriteChapterIds.contains(id),
                isBlocked = monetizationConfig.restrictChapters
                        && !monetizationConfig.availableChapterIds.contains(id)
                        && !premiumEnabled
            )

            updateState {
                State(
                    searchText = searchText,
                    selectedTags = tags.filter {
                        selectedTagIds.contains(it.id)
                    }.map {
                        Tag(
                            id = it.id,
                            name = it.name
                        )
                    },
                    items = mutableListOf<Item>().apply {
                        val filteredChapters = chapters.let {
                            if (selectedTagIds.isEmpty()) it
                            else it.filterByTagIds(selectedTagIds)
                        }.let {
                            if (searchText.isNullOrEmpty()) it
                            else it.filterByName(searchText)
                        }.sortedBy { it.id }

                        add(Item.Chest)
                        val favorites = filteredChapters.filter { favoriteChapterIds.contains(it.id) }

                        if (favorites.isNotEmpty()) {
                            add(Item.FavoritesGroup)
                            addAll(
                                favorites.map {
                                    it.toItem()
                                }
                            )
                        }

                        chapterGroups.sortedBy { it.id }.forEach { group ->
                            val chaptersOfGroup = filteredChapters.filter { group.chapterIds.contains(it.id) }

                            if (chaptersOfGroup.isNotEmpty()) {
                                add(Item.Group(group.name))
                                addAll(
                                    chaptersOfGroup.map {
                                        it.toItem()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }.launch()
    }

    fun setChapterFavorite(chapterId: Int, isFavorite: Boolean) {
        favoriteChapterIdsRepository.setFavoriteChapterId(chapterId, isFavorite)
    }

    fun setSearchText(text: String?) {
        searchTextStateFlow.value = text
    }

    fun setSelectedTagIds(ids: List<Int>) {
        selectedTagIdsStateFlow.value = ids
    }

    fun searchStateFlow() = searchTextStateFlow.asStateFlow()

    fun selectedTagsStateFlow() = selectedTagIdsStateFlow.asStateFlow()

    private fun List<ChapterData>.filterByName(text: String) = text.trim().split(" ").let { textParts ->
        filter { chapter ->
            textParts.all { chapter.name.contains(it, ignoreCase = true) }
        }
    }

    private fun List<ChapterData>.filterByTagIds(tags: List<Int>) = filter {
        it.tagIds.any(tags::contains)
    }

    data class State(
        val searchText: String?,
        val selectedTags: List<Tag>,
        val items: List<Item>
    )

    data class Tag(
        val id: Int,
        val name: String
    )

    sealed class Item {
        data class Chapter(
            val id: Int,
            val name: String,
            val isFavorite: Boolean,
            val isBlocked: Boolean
        ) : Item()

        data class Group(val name: String) : Item()

        object FavoritesGroup : Item()

        object Chest : Item()
    }
}