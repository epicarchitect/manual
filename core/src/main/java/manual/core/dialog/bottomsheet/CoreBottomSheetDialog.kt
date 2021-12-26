package manual.core.dialog.bottomsheet

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import manual.core.os.getOr
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import manual.core.R

abstract class CoreBottomSheetDialog(
    context: Context,
    theme: Int,
    private val initialBehaviorState: Int = BottomSheetBehavior.STATE_COLLAPSED,
    private val fullscreen: Boolean = false
) : BottomSheetDialog(context, theme) {

    protected var restoredInstanceState: Bundle? = null
        private set

    protected val bottomSheetLayout get() = checkNotNull(findViewById<FrameLayout>(R.id.design_bottom_sheet))

    override fun onSaveInstanceState() = super.onSaveInstanceState().apply {
        putInt(SavedInstanceArgument.BEHAVIOR_STATE, behavior.state)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoredInstanceState = savedInstanceState
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        bottomSheetLayout.updateLayoutParams {
            height = if (fullscreen) MATCH_PARENT else WRAP_CONTENT
        }
    }

    override fun onStart() {
        behavior.state = restoredInstanceState.getOr(SavedInstanceArgument.BEHAVIOR_STATE) { initialBehaviorState }
    }

    private object SavedInstanceArgument {
        const val BEHAVIOR_STATE = "BEHAVIOR_STATE"
    }
}