package manual.app.data

data class MonetizationConfig(
    val showInterstitialAds: Boolean,
    val showNativeAds: Boolean,
    val restrictChapters: Boolean,
    val unblockChaptersForRewardedAd: Boolean,
    val rewardedChapterIds: List<Int>,
    val availableChapterIds: List<Int>,
)