@file:Suppress("UNCHECKED_CAST")

package manual.app.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import manual.app.data.ChapterTags
import manual.app.data.MonetizationConfig
import manual.app.data.NotesConfig
import manual.app.premium.PremiumManager
import manual.app.repository.*
import manual.core.viewmodel.CoreViewModel
import kotlin.reflect.KClass
import manual.app.data.Chapter as ChapterData
import manual.app.data.ChapterGroup as ChapterGroupData
import manual.app.data.Tag as TagData

class ChaptersViewModel(
    chaptersRepository: ChaptersRepository,
    premiumManager: PremiumManager,
    monetizationConfigRepository: MonetizationConfigRepository,
    chapterGroupsRepository: ChapterGroupsRepository,
    tagsRepository: TagsRepository,
    unblockedChapterIdsRepository: UnblockedChapterIdsRepository,
    chapterTagsRepository: ChapterTagsRepository,
    notesRepository: NotesRepository,
    private val favoriteChapterIdsRepository: FavoriteChapterIdsRepository
) : CoreViewModel<ChaptersViewModel.State>() {

    private val searchTextState = MutableStateFlow<String?>(null)
    private val selectedTagIdsState = MutableStateFlow<List<Int>>(emptyList())
    private val groupIdsStack = MutableStateFlow(listOf(ROOT_GROUP_ID))
    private val searchTypeState =
        MutableStateFlow<KClass<out SearchState>>(SearchState.ByGroups::class)

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
            unblockedChapterIdsRepository.unblockedChapterIdsFlow(),
            tagsRepository.tagsFlow(),
            chapterTagsRepository.chapterTagsFlow(),
            notesRepository.configFlow()
        ) {
            val chapterDatas = it[0] as List<ChapterData>
            val chapterGroupDatas = it[1] as List<ChapterGroupData>
            val selectedTagIds = it[2] as List<Int>
            val searchText = it[3] as String?
            val groupIdsStack = it[4] as List<Int>
            val searchType = it[5] as KClass<out SearchState>
            val premiumEnabled = it[6] as Boolean
            val monetizationConfig = it[7] as MonetizationConfig
            val favoriteChapterIds = it[8] as List<Int>
            val unblockedChapterIds = it[9] as List<Int>
            val tagDatas = it[10] as List<TagData>
            val chapterTags = it[11] as List<ChapterTags>
            val notesConfig = it[12] as NotesConfig

            fun ChapterData.toItem() = Item.Chapter(
                id = id,
                name = name,
                isFavorite = favoriteChapterIds.contains(id),
                isBlocked = monetizationConfig.restrictChapters
                        && !unblockedChapterIds.contains(id)
                        && !monetizationConfig.availableChapterIds.contains(id)
                        && !premiumEnabled,
                canUnblockByAd = monetizationConfig.restrictChapters
                        && monetizationConfig.unblockChaptersForRewardedAd
                        && monetizationConfig.rewardedChapterIds.contains(id)
                        && !monetizationConfig.availableChapterIds.contains(id)
                        && !premiumEnabled
            )

            updateState {
                val availableSearchTypes = mutableListOf<KClass<out SearchState>>().apply {
                    if (chapterGroupDatas.isNotEmpty()) {
                        add(SearchState.ByGroups::class)
                    }

                    if (tagDatas.isNotEmpty()) {
                        add(SearchState.ByTags::class)
                    }

                    add(SearchState.ByName::class)

                    if (favoriteChapterIds.isNotEmpty()) {
                        add(SearchState.ByFavorites::class)
                    }
                }

                val items = mutableListOf<Item>().apply {
                    when (searchType) {
                        SearchState.ByGroups::class -> {
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

                                if (notesConfig.positionInChaptersByGroups >= 0) {
                                    add(
                                        notesConfig.positionInChaptersByGroups,
                                        Item.NotesButtonItem
                                    )
                                }
                            } else {
                                val currentGroupData =
                                    chapterGroupDatas.first { it.id == currentGroupId }

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

                        SearchState.ByName::class -> {
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

                        SearchState.ByTags::class -> {
                            addAll(
                                chapterDatas.let {
                                    if (selectedTagIds.isEmpty()) it
                                    else it.filterByTagIds(chapterTags, selectedTagIds)
                                }.map {
                                    it.toItem()
                                }.sortedBy {
                                    it.id
                                }
                            )
                            if (selectedTagIds.isEmpty() && notesConfig.positionInChaptersByTags >= 0) {
                                add(
                                    notesConfig.positionInChaptersByTags,
                                    Item.NotesButtonItem
                                )
                            }
                        }

                        SearchState.ByFavorites::class -> {
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
                                if (skippedForNativeAd == 8) {
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
                    SearchState.ByGroups::class -> {
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

                    SearchState.ByName::class -> {
                        State(
                            searchState = SearchState.ByName(searchText),
                            items = items,
                            canNavigateBack = availableSearchTypes.first() != searchType,
                            availableSearchTypes = availableSearchTypes
                        )
                    }

                    SearchState.ByTags::class -> {
                        State(
                            searchState = SearchState.ByTags(
                                tagDatas.filter {
                                    selectedTagIds.contains(it.id)
                                }.map {
                                    Tag(
                                        id = it.id,
                                        name = it.name
                                    )
                                },
                                isBlocked = !premiumEnabled && monetizationConfig.restrictSearchByTags
                            ),
                            items = items,
                            canNavigateBack = availableSearchTypes.first() != searchType,
                            availableSearchTypes = availableSearchTypes
                        )
                    }

                    SearchState.ByFavorites::class -> {
                        State(
                            searchState = SearchState.ByFavorites(),
                            items = items,
                            canNavigateBack = availableSearchTypes.first() != searchType,
                            availableSearchTypes = availableSearchTypes
                        )
                    }

                    else -> error("Unexpected search type: ${searchType::class}")
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

    fun setSearchType(type: KClass<out SearchState>) {
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

    private fun List<ChapterData>.filterByName(text: String) =
        text.trim().split(" ").let { textParts ->
            filter { chapter ->
                textParts.all { chapter.name.contains(it, ignoreCase = true) }
            }
        }

    private fun List<ChapterData>.filterByTagIds(
        chapterTags: List<ChapterTags>,
        selectedTags: List<Int>
    ) = filter { chapter ->
        chapterTags.firstOrNull {
            it.chapterId == chapter.id
        }?.tagIds?.any(selectedTags::contains) ?: false
    }

    data class State(
        val searchState: SearchState,
        val items: List<Item>,
        val canNavigateBack: Boolean,
        val availableSearchTypes: List<KClass<out SearchState>>
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
            val isBlocked: Boolean,
            val canUnblockByAd: Boolean
        ) : Item()

        data class Group(val id: Int, val name: String) : Item()

        class NativeAd : Item()

        object NotesButtonItem : Item()

        object FavoritesGroup : Item()
    }

    sealed class SearchState {
        data class ByGroups(val currentGroupTitle: String?, val isRoot: Boolean) : SearchState()

        data class ByName(val name: String?) : SearchState()

        data class ByTags(val tags: List<Tag>, val isBlocked: Boolean) : SearchState()

        class ByFavorites : SearchState()
    }

    companion object {
        private const val ROOT_GROUP_ID = Int.MIN_VALUE
    }
}