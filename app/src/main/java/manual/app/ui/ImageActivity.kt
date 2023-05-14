package manual.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import kotlinx.parcelize.Parcelize
import manual.app.R
import manual.app.databinding.ImageActivityBinding
import manual.core.activity.CoreActivity

class ImageActivity : CoreActivity<ImageActivityBinding>(ImageActivityBinding::inflate) {

    override fun getThemeResourceId() = R.style.Activity

    override fun ImageActivityBinding.onCreated() {
        val args = getArgs()
        val fileExt = args.uri.pathSegments.last().split(".").last()
        if (fileExt == "gif") {
            gifImage.isVisible = true
            image.isVisible = false
            Glide.with(gifImage).load(args.uri).into(gifImage)
        } else {
            gifImage.isVisible = false
            image.isVisible = true
            image.setImage(ImageSource.uri(args.uri))
        }
        titleTextView.text = args.title
        backButton.setOnClickListener { finish() }
    }

    private fun getArgs(): Args = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.extras!!.getParcelable("args", Args::class.java)!!
    } else {
        intent.extras!!.getParcelable("args")!!
    }

    @Parcelize
    data class Args(
        val uri: Uri,
        val title: String?
    ) : Parcelable

    companion object {
        fun open(
            context: Context,
            uri: Uri,
            title: String? = null
        ) = context.startActivity(
            Intent(
                context,
                ImageActivity::class.java
            ).putExtra(
                "args",
                Args(
                    uri = uri,
                    title = title
                )
            )
        )
    }
}