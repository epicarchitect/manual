package manual.core.view

import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import androidx.core.text.getSpans

fun CharSequence.handleLinks(onClicked: (Uri) -> Unit) = SpannableStringBuilder.valueOf(this).apply {
    getSpans<URLSpan>().forEach { urlSpan ->
        val spanStart = getSpanStart(urlSpan)
        val spanEnd = getSpanEnd(urlSpan)
        val spanFlags = getSpanFlags(urlSpan)
        removeSpan(urlSpan)

        setSpan(
            object : ClickableSpan() {
                override fun onClick(view: View) {
                    onClicked(Uri.parse(urlSpan.url))
                }
            },
            spanStart,
            spanEnd,
            spanFlags
        )
    }
}!!