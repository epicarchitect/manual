package manual.app.data

data class MonetizationConfig(
    val showInterstitialAds: Boolean,
    val showNativeAds: Boolean,
    val restrictChapters: Boolean,
    val availableChapterIds: List<Int>,
)