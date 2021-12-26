package manual.core.resources

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat

val Int.dp get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Float.dp get() = this * Resources.getSystem().displayMetrics.density

val Int.sp get() = (this * Resources.getSystem().displayMetrics.scaledDensity).toInt()
val Float.sp get() = this * Resources.getSystem().displayMetrics.scaledDensity

fun Context.getResourceIdByAttributeId(@AttrRes attributeId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attributeId, typedValue, true)
    return typedValue.resourceId
}

fun Context.getColorByAttributeId(@AttrRes attributeId: Int) = ContextCompat.getColor(
    this,
    getResourceIdByAttributeId(attributeId)
)
