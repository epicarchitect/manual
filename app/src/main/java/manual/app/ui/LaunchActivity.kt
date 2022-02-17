package manual.app.ui

import android.content.Intent
import android.graphics.Color
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.bumptech.glide.Glide
import manual.app.R
import manual.app.databinding.LaunchActivityBinding
import manual.core.activity.CoreActivity

class LaunchActivity : CoreActivity<LaunchActivityBinding>(LaunchActivityBinding::inflate) {

    override fun getThemeResourceId() = R.style.Activity_Launcher

    override fun LaunchActivityBinding.onCreated() {
        Glide.with(root)
            .load(R.drawable.launch)
            .into(backgroundImageView)

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

    override fun onResume() {
        super.onResume()
        requireBinding().darkView.animate().alpha(0f).setDuration(2000).start()
    }

    override fun onPause() {
        super.onPause()
        requireBinding().darkView.animate().alpha(1f).setDuration(300).start()
    }
}