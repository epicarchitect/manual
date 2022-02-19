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
import java.util.*

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
                            items = mutableListOf<Item>().apply {
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
                            },
                            canNavigateBack = currentGroupId != ROOT_GROUP_ID
                        )
                    }
                    SearchType.BY_NAME -> {
                        State(
                            searchState = SearchState.ByName(searchText),
                            items = mutableListOf<Item>().apply {
                                addAll(
                                    chapterDatas.let {
                                        if (searchText.isNullOrEmpty()) it
                                        else it.filterByName(searchText)
                                    }.map {
                                        it.toItem()
                                    }
                                )
                            },
                            canNavigateBack = true
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
                            items = mutableListOf<Item>().apply {
                                addAll(
                                    chapterDatas.let {
                                        if (selectedTagIds.isEmpty()) it
                                        else it.filterByTagIds(selectedTagIds)
                                    }.map {
                                        it.toItem()
                                    }
                                )
                            },
                            canNavigateBack = true
                        )
                    }
                    SearchType.ONLY_FAVORITES -> {
                        State(
                            searchState = SearchState.OnlyFavorites(),
                            items = mutableListOf<Item>().apply {
                                addAll(
                                    chapterDatas.filter {
                                        favoriteChapterIds.contains(it.id)
                                    }.map {
                                        it.toItem()
                                    }
                                )
                            },
                            canNavigateBack = true
                        )
                    }
                }
            }
        }.launch()

//        combine(
//            chaptersRepository.chaptersFlow(),
//            chapterGroupsRepository.chapterGroupsFlow(),
//            selectedTagIdsStateFlow,
//            searchTextStateFlow,
//            premiumManager.premiumEnabledFlow().filterNotNull(),
//            monetizationConfigRepository.monetizationConfigFlow(),
//            favoriteChapterIdsRepository.favoriteChapterIdsFlow(),
//            tagsRepository.tagsFlow(),
//            groupIdsStackStateFlow
//        ) {
//            val chapters = it[0] as List<ChapterData>
//            val chapterGroups = it[1] as List<ChapterGroupData>
//            val selectedTagIds = it[2] as List<Int>
//            val searchText = it[3] as String?
//            val premiumEnabled = it[4] as Boolean
//            val monetizationConfig = it[5] as MonetizationConfig
//            val favoriteChapterIds = it[6] as List<Int>
//            val tags = it[7] as List<TagData>
//            val groupIdsStack = it[8] as List<Int>
//
//            fun ChapterData.toItem() = Item.Chapter(
//                id = id,
//                name = name,
//                isFavorite = favoriteChapterIds.contains(id),
//                isBlocked = monetizationConfig.restrictChapters
//                        && !monetizationConfig.availableChapterIds.contains(id)
//                        && !premiumEnabled
//            )
//
//            updateState {
//                State(
//                    searchText = searchText,
//                    selectedTags = tags.filter {
//                        selectedTagIds.contains(it.id)
//                    }.map {
//                        Tag(
//                            id = it.id,
//                            name = it.name
//                        )
//                    },
//                    items = mutableListOf<Item>().apply {
//                        val filteredChapters = chapters.let {
//                            if (selectedTagIds.isEmpty()) it
//                            else it.filterByTagIds(selectedTagIds)
//                        }.let {
//                            if (searchText.isNullOrEmpty()) it
//                            else it.filterByName(searchText)
//                        }
//
//                        val filteredChapterGroups = chapterGroups.let {
//                            if (selectedTagIds.isEmpty()) it
//                            else chapterGroups.filter { group ->
//                                filteredChapters.any { group.chapterIds.contains(it.id) }
//                            }
//                        }
//
//                        if (searchText == null) {
//                            add(Item.Chest)
//
//                            val favorites = filteredChapters.filter { favoriteChapterIds.contains(it.id) }
//
//                            if (favorites.isNotEmpty()) {
//                                add(Item.FavoritesGroup)
//                                addAll(
//                                    favorites.map {
//                                        it.toItem()
//                                    }
//                                )
//                            }
//
//                            fun addSubgroups(group: ChapterGroupData) {
//                                val subgroups = filteredChapterGroups.filter { group.subgroupIds.contains(it.id) }
//                                subgroups.sortedBy { it.id }.forEach { subgroup ->
//                                    val chaptersOfSubGroup = filteredChapters.filter { subgroup.chapterIds.contains(it.id) }
//
//                                    if (chaptersOfSubGroup.isNotEmpty() || subgroup.subgroupIds.isNotEmpty()) {
//                                        val isExpanded = expandedSubgroupIds.contains(subgroup.id)
//
//                                        add(
//                                            Item.Subgroup(
//                                                subgroup.id,
//                                                subgroup.name,
//                                                isExpanded
//                                            )
//                                        )
//
//                                        if (isExpanded) {
//                                            addAll(
//                                                chaptersOfSubGroup.map {
//                                                    it.toItem()
//                                                }
//                                            )
//                                            if (subgroup.subgroupIds.isNotEmpty()) {
//                                                addSubgroups(subgroup)
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//
//                            val rootGroups = filteredChapterGroups.filter { group ->
//                                filteredChapterGroups.all { !it.subgroupIds.contains(group.id) }
//                            }
//
//                            var skippedGroupsForAd = 0
//
//                            rootGroups.sortedBy { it.id }.forEach { group ->
//                                val chaptersOfGroup = filteredChapters.filter { group.chapterIds.contains(it.id) }
//
//                                if (chaptersOfGroup.isNotEmpty() || group.subgroupIds.isNotEmpty()) {
//                                    if (skippedGroupsForAd > 0) {
//                                        if (!premiumEnabled && monetizationConfig.showNativeAds) {
//                                            add(Item.NativeAd())
//                                            skippedGroupsForAd = 0
//                                        }
//                                    } else {
//                                        skippedGroupsForAd++
//                                    }
//                                    add(Item.Group(group.name))
//                                    addAll(
//                                        chaptersOfGroup.map {
//                                            it.toItem()
//                                        }
//                                    )
//                                    if (group.subgroupIds.isNotEmpty()) {
//                                        addSubgroups(group)
//                                    }
//                                }
//                            }
//                        } else {
//                            addAll(
//                                filteredChapters.map {
//                                    it.toItem()
//                                }.filter { it.id != -1 }
//                            )
//                        }
//                    }
//                )
//            }
//        }.launch()
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
                    is SearchState.ByName -> {
                        searchTypeState.value = SearchType.BY_GROUPS
                    }
                    is SearchState.ByTags -> {
                        searchTypeState.value = SearchType.BY_GROUPS
                    }
                    is SearchState.OnlyFavorites -> {
                        searchTypeState.value = SearchType.BY_GROUPS
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
        val canNavigateBack: Boolean
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
        data class ByGroups(val currentGroupTitle: String?, val isRoot: Boolean) : SearchState()
        data class ByName(val name: String?) : SearchState()
        data class ByTags(val tags: List<Tag>) : SearchState()
        class OnlyFavorites : SearchState()
    }

    enum class SearchType {
        BY_GROUPS,
        BY_NAME,
        BY_TAGS,
        ONLY_FAVORITES,
    }

    companion object {
        private const val ROOT_GROUP_ID = Int.MIN_VALUE
        private val defaultSearchByGroupsState = SearchState.ByGroups(null, true)
        private val defaultSearchByNameState = SearchState.ByName("")
        private val defaultSearchByTagsState = SearchState.ByTags(emptyList())
    }
}