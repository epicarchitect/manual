package manual.app.data

data class MonetizationConfig(
    val unblockContentsByAds: Boolean,
    val restrictChapters: Boolean,
    val restrictContents: Boolean,
    val availableChapterIds: List<Int>,
    val availableContentIds: List<Int>
)