package manual.app.ui

import android.content.Intent
import android.net.Uri
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import manual.app.R
import manual.app.databinding.LaunchActivityBinding
import manual.app.repository.LaunchConfigRepository
import manual.core.activity.CoreActivity
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import org.koin.android.ext.android.inject

class LaunchActivity : CoreActivity<LaunchActivityBinding>(LaunchActivityBinding::inflate) {

    private val launchConfigRepository: LaunchConfigRepository by inject()

    override fun getThemeResourceId() = R.style.Activity_Launcher

    override fun LaunchActivityBinding.onCreated() {
        launchConfigRepository.launchConfigFlow().onEachChanged {
            when (it.backgroundSource.split(".").last()) {
                "mp4" -> {
                    backgroundImageView.isVisible = false
                    playerView.isVisible = true
                    playerView.player = ExoPlayer.Builder(root.context).build().apply {
                        playWhenReady = true
                        repeatMode = ExoPlayer.REPEAT_MODE_ALL
                        setMediaItem(
                            MediaItem.fromUri(
                                Uri.parse("asset:///${it.backgroundSource}")
                            )
                        )
                        prepare()
                        play()
                    }
                }
                else -> {
                    backgroundImageView.isVisible = true
                    playerView.isVisible = false
                    Glide.with(root)
                        .load(Uri.parse("file:///android_asset/${it.backgroundSource}"))
                        .into(backgroundImageView)
                }
            }
        }.launchWith(this@LaunchActivity)

        nextButton.startAnimation(
            AlphaAnimation(0.2f, 0.9f).apply {
                duration = 2000
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
        )

        root.setOnClickListener {
            it.isClickable = false
            darkView.animate()
                .alpha(1f)
                .setDuration(500)
                .withEndAction {
                    finish()
                    overridePendingTransition(0, 0)
                    startActivity(Intent(this@LaunchActivity, AppActivity::class.java))
                }
                .start()
        }
    }

    override fun onDestroy() {
        requireBinding().playerView.player?.release()
        requireBinding().playerView.player = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        requireBinding().darkView.animate().alpha(0f).setDuration(2000).start()
    }
}