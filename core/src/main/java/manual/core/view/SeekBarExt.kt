package manual.core.view

import android.widget.SeekBar

fun SeekBar.onStopProgressChanging(onChanged: (Int) -> Unit) {
    var lastProgress = progress
    setOnSeekBarChangeListener(
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = Unit

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (lastProgress != progress) {
                    lastProgress = progress
                    onChanged(progress)
                }
            }
        }
    )
}