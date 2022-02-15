package manual.app.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import manual.app.repository.TagGroupsRepository
import manual.app.repository.TagsRepository
import manual.core.viewmodel.CoreViewModel

class TagSelectionViewModel(
    tagsRepository: TagsRepository,
    tagGroupsRepository: TagGroupsRepository,
    selectedTagIds: List<Int>
) : CoreViewModel<TagSelectionViewModel.State>() {

    private val selectedIdsStateFlow = MutableStateFlow<Set<Int>>(HashSet(selectedTagIds))

    init {
        combine(
            tagsRepository.tagsFlow(),
            tagGroupsRepository.tagGroupsFlow(),
            selectedIdsStateFlow
        ) { tags, groups, selectedIds ->
            updateState {
                State(
                    items = mutableListOf<Item>().apply {
                        groups.sortedBy { it.id }.forEach { group ->
                            val tagsOfGroup = tags.filter { group.tagIds.contains(it.id) }
                            if (tagsOfGroup.isNotEmpty()) {
                                add(Item.Group(group.name))
                                addAll(
                                    tagsOfGroup.sortedBy {
                                        it.id
                                    }.map {
                                        Item.Tag(
                                            it.id,
                                            it.name,
                                            selectedIds.contains(it.id)
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }.launch()
    }

    fun setTagSelected(tagId: Int, isSelected: Boolean) = launch {
        selectedIdsStateFlow.value = HashSet(selectedIdsStateFlow.value).apply {
            if (isSelected) {
                add(tagId)
            } else {
                remove(tagId)
            }
        }
    }

    fun selectAll() = launch {
        selectedIdsStateFlow.value = HashSet(selectedIdsStateFlow.value).apply {
            state.value?.items?.forEach {
                if (it is Item.Tag) {
                    add(it.id)
                }
            }
        }
    }

    fun clearAll() = launch {
        selectedIdsStateFlow.value = emptySet()
    }

    fun getSelectedTagIds() = selectedIdsStateFlow.value.toList()

    class State(
        val items: List<Item>
    )

    sealed class Item {
        data class Tag(
            val id: Int,
            val name: String,
            val isChecked: Boolean
        ) : Item()

        data class Group(val name: String) : Item()
    }
}