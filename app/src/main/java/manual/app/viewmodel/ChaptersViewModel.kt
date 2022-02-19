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

    private val searchTextState = MutableStateFlow<String?>(null)
    private val selectedTagIdsState = MutableStateFlow<List<Int>>(emptyList())
    private val groupIdsStack = MutableStateFlow(listOf(ROOT_GROUP_ID))
    private val searchTypeState = MutableStateFlow(SearchType.BY_GROUPS)

    init {
        combine(
            chaptersRepository.chaptersFlow(),
            chapterGroupsRepository.chapterGroupsFlow(),
            selectedTagIdsState,
            searchTextState,
            groupIdsStack,
            searchTypeState,
            premiumManager.premiumEnabledFlow().filterNotNull(),
            monetizationConfigRepository.monetizationConfigFlow(),
            favoriteChapterIdsRepository.favoriteChapterIdsFlow(),
            tagsRepository.tagsFlow(),
        ) {
            val chapterDatas = it[0] as List<ChapterData>
            val chapterGroupDatas = it[1] as List<ChapterGroupData>
            val selectedTagIds = it[2] as List<Int>
            val searchText = it[3] as String?
            val groupIdsStack = it[4] as List<Int>
            val searchType = it[5] as SearchType
            val premiumEnabled = it[6] as Boolean
            val monetizationConfig = it[7] as MonetizationConfig
            val favoriteChapterIds = it[8] as List<Int>
            val tagDatas = it[9] as List<TagData>

            fun ChapterData.toItem() = Item.Chapter(
                id = id,
                name = name,
                isFavorite = favoriteChapterIds.contains(id),
                isBlocked = monetizationConfig.restrictChapters
                        && !monetizationConfig.availableChapterIds.contains(id)
                        && !premiumEnabled
            )

            updateState {
                val availableSearchTypes = mutableListOf<SearchType>().apply {
                    if (chapterGroupDatas.isNotEmpty()) {
                        add(SearchType.BY_GROUPS)
                    }

                    if (tagDatas.isNotEmpty()) {
                        add(SearchType.BY_TAGS)
                    }

                    add(SearchType.BY_NAME)

                    if (favoriteChapterIds.isNotEmpty()) {
                        add(SearchType.BY_FAVORITES)
                    }
                }

                val items = mutableListOf<Item>().apply {
                    when (searchType) {
                        SearchType.BY_GROUPS -> {
                            val currentGroupId = groupIdsStack.last()
                            if (currentGroupId == ROOT_GROUP_ID) {
                                val subgroupIds = mutableSetOf<Int>()
                                val subgroupChapterIds = mutableSetOf<Int>()
                                chapterGroupDatas.forEach {
                                    subgroupIds.addAll(it.subgroupIds)
                                    subgroupChapterIds.addAll(it.chapterIds)
                                }

                                val rootChapterGroupDatas = chapterGroupDatas.filterNot {
                                    subgroupIds.contains(it.id)
                                }

                                val rootChapterDatas = chapterDatas.filterNot {
                                    subgroupChapterIds.contains(it.id)
                                }

                                addAll(
                                    rootChapterDatas.map {
                                        it.toItem()
                                    }.sortedBy {
                                        it.id
                                    }
                                )

                                addAll(
                                    rootChapterGroupDatas.map {
                                        Item.Group(
                                            it.id,
                                            it.name
                                        )
                                    }.sortedBy {
                                        it.id
                                    }
                                )
                            } else {
                                val currentGroupData = chapterGroupDatas.first { it.id == currentGroupId }

                                addAll(
                                    chapterDatas.filter {
                                        currentGroupData.chapterIds.contains(it.id)
                                    }.map {
                                        it.toItem()
                                    }.sortedBy {
                                        it.id
                                    }
                                )

                                addAll(
                                    chapterGroupDatas.filter {
                                        currentGroupData.subgroupIds.contains(it.id)
                                    }.map {
                                        Item.Group(
                                            it.id,
                                            it.name
                                        )
                                    }.sortedBy {
                                        it.id
                                    }
                                )
                            }
                        }
                        SearchType.BY_NAME -> {
                            addAll(
                                chapterDatas.let {
                                    if (searchText.isNullOrEmpty()) it
                                    else it.filterByName(searchText)
                                }.map {
                                    it.toItem()
                                }.sortedBy {
                                    it.id
                                }
                            )
                        }
                        SearchType.BY_TAGS -> {
                            addAll(
                                chapterDatas.let {
                                    if (selectedTagIds.isEmpty()) it
                                    else it.filterByTagIds(selectedTagIds)
                                }.map {
                                    it.toItem()
                                }.sortedBy {
                                    it.id
                                }
                            )
                        }
                        SearchType.BY_FAVORITES -> {
                            addAll(
                                chapterDatas.filter {
                                    favoriteChapterIds.contains(it.id)
                                }.map {
                                    it.toItem()
                                }.sortedBy {
                                    it.id
                                }
                            )
                        }
                    }
                }.let { items ->
                    if (premiumEnabled || !monetizationConfig.showNativeAds) {
                        items
                    } else {
                        var skippedForNativeAd = 0

                        mutableListOf<Item>().apply {
                            items.forEach {
                                add(it)
                                if (skippedForNativeAd == 4) {
                                    add(Item.NativeAd())
                                    skippedForNativeAd = 0
                                } else {
                                    skippedForNativeAd++
                                }
                            }
                        }
                    }
                }

                when (searchType) {
                    SearchType.BY_GROUPS -> {
                        val currentGroupId = groupIdsStack.last()

                        State(
                            searchState = when (currentGroupId) {
                                ROOT_GROUP_ID -> {
                                    SearchState.ByGroups(null, true)
                                }
                                else -> {
                                    val group = chapterGroupDatas.first { it.id == currentGroupId }
                                    SearchState.ByGroups(group.name, false)
                                }
                            },
                            items = items,
                            canNavigateBack = currentGroupId != ROOT_GROUP_ID,
                            availableSearchTypes = availableSearchTypes
                        )
                    }
                    SearchType.BY_NAME -> {
                        State(
                            searchState = SearchState.ByName(searchText),
                            items = items,
                            canNavigateBack = availableSearchTypes.first() != searchType,
                            availableSearchTypes = availableSearchTypes
                        )
                    }
                    SearchType.BY_TAGS -> {
                        State(
                            searchState = SearchState.ByTags(
                                tagDatas.filter {
                                    selectedTagIds.contains(it.id)
                                }.map {
                                    Tag(
                                        id = it.id,
                                        name = it.name
                                    )
                                }
                            ),
                            items = items,
                            canNavigateBack = availableSearchTypes.first() != searchType,
                            availableSearchTypes = availableSearchTypes
                        )
                    }
                    SearchType.BY_FAVORITES -> {
                        State(
                            searchState = SearchState.ByFavorites(),
                            items = items,
                            canNavigateBack = availableSearchTypes.first() != searchType,
                            availableSearchTypes = availableSearchTypes
                        )
                    }
                }
            }
        }.launch()
    }

    fun setChapterFavorite(chapterId: Int, isFavorite: Boolean) {
        favoriteChapterIdsRepository.setFavoriteChapterId(chapterId, isFavorite)
    }

    fun setSearchText(text: String?) {
        searchTextState.value = text
    }

    fun setSearchType(type: SearchType) {
        searchTypeState.value = type
    }

    fun navigateInGroup(groupId: Int) {
        groupIdsStack.value += groupId
    }

    fun navigateBack() {
        state.value?.let { state ->
            if (state.canNavigateBack) {
                when (state.searchState) {
                    is SearchState.ByGroups -> {
                        groupIdsStack.value = groupIdsStack.value.dropLast(1)
                    }
                    else -> {
                        searchTypeState.value = state.availableSearchTypes.first()
                    }
                }
            }
        }
    }

    fun setSelectedTagIds(ids: List<Int>) {
        selectedTagIdsState.value = ids
    }

    private fun List<ChapterData>.filterByName(text: String) = text.trim().split(" ").let { textParts ->
        filter { chapter ->
            textParts.all { chapter.name.contains(it, ignoreCase = true) }
        }
    }

    private fun List<ChapterData>.filterByTagIds(tags: List<Int>) = filter {
        it.tagIds.any(tags::contains)
    }

    data class State(
        val searchState: SearchState,
        val items: List<Item>,
        val canNavigateBack: Boolean,
        val availableSearchTypes: List<SearchType>
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

        data class Group(val id: Int, val name: String) : Item()

        class NativeAd : Item()

        object FavoritesGroup : Item()
    }

    sealed class SearchState {
        abstract val searchType: SearchType

        data class ByGroups(val currentGroupTitle: String?, val isRoot: Boolean) : SearchState() {
            override val searchType = SearchType.BY_GROUPS
        }

        data class ByName(val name: String?) : SearchState() {
            override val searchType = SearchType.BY_NAME
        }

        data class ByTags(val tags: List<Tag>) : SearchState() {
            override val searchType = SearchType.BY_TAGS
        }

        class ByFavorites : SearchState() {
            override val searchType = SearchType.BY_FAVORITES
        }
    }

    enum class SearchType {
        BY_GROUPS,
        BY_NAME,
        BY_TAGS,
        BY_FAVORITES,
    }

    companion object {
        private const val ROOT_GROUP_ID = Int.MIN_VALUE
        private val defaultSearchByGroupsState = SearchState.ByGroups(null, true)
        private val defaultSearchByNameState = SearchState.ByName("")
        private val defaultSearchByTagsState = SearchState.ByTags(emptyList())
    }
}