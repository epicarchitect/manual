package manual.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import manual.app.R
import manual.app.databinding.ChapterAudioItemBinding
import manual.app.databinding.ChapterImageItemBinding
import manual.app.databinding.ChapterTextItemBinding
import manual.app.databinding.ChestFragmentBinding
import manual.app.viewmodel.ChapterViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.CoreFragment
import manual.core.view.BindingRecyclerViewAdapterBuilder
import manual.core.view.buildBindingRecyclerViewAdapter
import manual.core.view.handleLinks
import manual.core.view.requireBindingRecyclerViewAdapter
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChestFragment(
    private val delegate: Delegate
) : CoreFragment<ChestFragmentBinding>(ChestFragmentBinding::inflate) {

    private val viewModel: ChapterViewModel by viewModel { parametersOf(-1) }
    private val audioAssetPlayer: AudioAssetPlayer by inject()

    override fun ChestFragmentBinding.onCreated() {
        contentsRecyclerView.itemAnimator = null
        contentsRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setupHtmlContent()
            setupImageContent()
            setupAudioContent()
        }

        viewModel.state.map { it?.contents }.onEachChanged {
            contentsRecyclerView.requireBindingRecyclerViewAdapter().loadItems(
                it?.filterNot { it is ChapterViewModel.Content.NativeAd } ?: emptyList()
            )
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.title }.onEachChanged {
            titleTextView.text = it
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it == null }.onEachChanged {
            progressBar.isVisible = it
            contentsRecyclerView.isVisible = !it
        }.launchWith(viewLifecycleOwner)
    }

    private fun BindingRecyclerViewAdapterBuilder.setupHtmlContent() =
        setup<ChapterViewModel.Content.Html, ChapterTextItemBinding>(ChapterTextItemBinding::inflate) {
            bind { item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                textView.movementMethod = BetterLinkMovementMethod.getInstance()
                textView.text = item.html.handleLinks {
                    if (it.scheme == "manual") {
                        delegate.navigateToChapter(this@ChestFragment, it.pathSegments.last().toInt())
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, it))
                    }
                }
            }
        }

    private fun BindingRecyclerViewAdapterBuilder.setupImageContent() =
        setup<ChapterViewModel.Content.Image, ChapterImageItemBinding>(ChapterImageItemBinding::inflate) {
            bind { item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                Glide.with(root).load(item.uri).into(imageView)
                imageView.setOnClickListener {
                    StfalconImageViewer.Builder(context, listOf(item.uri)) { view, uri ->
                        Glide.with(root).load(uri).into(view)
                    }.withHiddenStatusBar(false).show()
                }
            }
        }

    @SuppressLint("SetTextI18n")
    private fun BindingRecyclerViewAdapterBuilder.setupAudioContent() =
        setup<ChapterViewModel.Content.Audio, ChapterAudioItemBinding>(ChapterAudioItemBinding::inflate) {
            bind { scope, item ->
                nameTextView.isVisible = item.name.isNotEmpty()
                nameTextView.text = item.name
                audioAssetPlayer.state.onEach {
                    if (it?.path == item.source) {
                        if (it.isPlaying) {
                            playButton.setImageResource(R.drawable.ic_pause)
                            playButton.setOnClickListener {
                                audioAssetPlayer.pause()
                            }
                        } else {
                            playButton.setImageResource(R.drawable.ic_play)
                            playButton.setOnClickListener {
                                audioAssetPlayer.resume()
                            }
                        }

                        seekBar.isEnabled = true
                        seekBar.max = it.duration
                        seekBar.progress = it.position
                        seekBar.setOnSeekBarChangeListener(
                            object : SeekBar.OnSeekBarChangeListener {
                                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                    if (fromUser) {
                                        audioAssetPlayer.seekTo(progress)
                                    }
                                }
                            }
                        )

                        positionTextView.isVisible = true
                        durationTextView.isVisible = true
                        durationDividerTextView.isVisible = true
                        positionTextView.text = formatAudioTime(it.position)
                        durationTextView.text = formatAudioTime(it.duration)
                    } else {
                        seekBar.setOnSeekBarChangeListener(null)
                        seekBar.isEnabled = false
                        seekBar.progress = 0
                        playButton.setImageResource(R.drawable.ic_play)
                        playButton.setOnClickListener {
                            audioAssetPlayer.play(item.source)
                        }
                        positionTextView.isVisible = false
                        durationTextView.isVisible = false
                        durationDividerTextView.isVisible = false
                    }
                }.launchIn(scope)
            }
        }

    private fun formatAudioTime(time: Int): String = buildString {
        val seconds = time / 1000 % 60
        val minutes = time / 1000 / 60

        append(minutes)
        append(":")
        if (seconds < 10) {
            append(0)
        }
        append(seconds)
    }

    interface Delegate {
        fun navigateToChapter(fragment: ChestFragment, chapterId: Int)
    }
}