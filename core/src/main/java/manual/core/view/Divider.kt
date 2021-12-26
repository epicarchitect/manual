package manual.core.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import manual.core.resources.getColorByAttributeId
import kotlin.math.roundToInt

class Divider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        setBackgroundColor(context.getColorByAttributeId(android.R.attr.colorControlNormal))
        alpha = 0.4f
        minimumHeight = (0.8f * resources.displayMetrics.density).roundToInt()
    }
}