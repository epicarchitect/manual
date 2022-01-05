package manual.app.data

data class MonetizationConfig(
    val showAds: Boolean,
    val restrictChapters: Boolean,
    val availableChapterIds: List<Int>,
)