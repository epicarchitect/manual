package manual.app.data

data class Chapter(
    val id: Int,
    val name: String,
    val tagIds: List<Int>,
    val contentIds: List<Int>
)