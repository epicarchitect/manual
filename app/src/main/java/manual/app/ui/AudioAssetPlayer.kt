package manual.app.ui

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioAssetPlayer(
    private val application: Application,
    coroutineScope: CoroutineScope
) {

    private var player: MediaPlayer? = null
    private val mutableState = MutableStateFlow<State?>(null)
    val state = mutableState.asStateFlow()

    init {
        coroutineScope.launch {
            while (isActive) {
                runCatching {
                    mutableState.value = mutableState.value?.let {
                        val position = player!!.currentPosition
                        val duration = player!!.duration

                        it.copy(
                            position = position,
                            duration = duration,
                            isPlaying = if (position == duration) false else it.isPlaying
                        )
                    }
                }

                delay(100)
            }
        }
    }

    fun resume() {
        player?.start()
        mutableState.value = mutableState.value?.copy(
            isPlaying = true
        )
    }

    fun play(path: String) {
        release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            application.assets.openFd(path).use {
                setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }

            mutableState.value = State(
                path = path,
                position = 0,
                duration = 0,
                isPlaying = false
            )

            prepareAsync()
            setOnPreparedListener {
                start()
                mutableState.value?.let {
                    mutableState.value = it.copy(
                        isPlaying = true
                    )
                }
            }
        }
    }

    fun seekTo(value: Int) {
        player?.seekTo(value)
    }

    fun pause() {
        player?.pause()
        mutableState.value?.let {
            mutableState.value = it.copy(
                isPlaying = false
            )
        }
    }

    fun release() {
        player?.let {
            it.stop()
            it.release()
            player = null
            mutableState.value = null
        }
    }

    data class State(
        val path: String,
        val position: Int,
        val duration: Int,
        val isPlaying: Boolean
    )
}