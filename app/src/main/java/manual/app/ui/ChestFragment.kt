package manual.app.ui

import android.content.Intent
import androidx.core.view.isVisible
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.flow.map
import manual.app.databinding.ChapterImageItemBinding
import manual.app.databinding.ChapterTextItemBinding
import manual.app.databinding.ChestFragmentBinding
import manual.app.viewmodel.ChapterViewModel
import manual.core.coroutines.flow.launchWith
import manual.core.coroutines.flow.onEachChanged
import manual.core.fragment.CoreFragment
import manual.core.view.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChestFragment(
    private val delegate: Delegate
) : CoreFragment<ChestFragmentBinding>(ChestFragmentBinding::inflate) {

    private val viewModel: ChapterViewModel by viewModel { parametersOf(-1) }

    override fun ChestFragmentBinding.onCreated() {
        contentsRecyclerView.itemAnimator = null
        contentsRecyclerView.adapter = buildBindingRecyclerViewAdapter(viewLifecycleOwner) {
            setupHtmlContent()
            setupImageContent()
        }

        viewModel.state.map { it?.contents }.onEachChanged {
            contentsRecyclerView.requireBindingRecyclerViewAdapter().loadItems(it ?: emptyList())
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it?.title }.onEachChanged {
            titleTextView.text = it
        }.launchWith(viewLifecycleOwner)

        viewModel.state.map { it == null }.onEachChanged {
            progressBar.isVisible = it
            appBarLayout.isVisible = !it
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

                unblockLayout.isVisible = false
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

    interface Delegate {
        fun navigateToChapter(fragment: ChestFragment, chapterId: Int)
    }
}