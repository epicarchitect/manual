package manual.app.ui

import com.bumptech.glide.Glide
import manual.app.R
import manual.app.databinding.LaunchFragmentBinding
import manual.core.fragment.CoreFragment
import android.view.animation.Animation

import android.view.animation.AlphaAnimation


class LaunchFragment(
    private val delegate: Delegate
) : CoreFragment<LaunchFragmentBinding>(LaunchFragmentBinding::inflate) {

    override fun LaunchFragmentBinding.onCreated() {
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
            delegate.onNext()
        }
    }

    interface Delegate {
        fun onNext()
    }
}