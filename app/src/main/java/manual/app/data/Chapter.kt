package manual.app.data

data class Chapter(
    val id: Int,
    val name: String,
    val contents: List<Content>
)