package manual.core.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import manual.core.os.getOr
import java.util.concurrent.TimeUnit

abstract class CoreFragment<BINDING : ViewBinding>(
    private val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> BINDING
) : Fragment() {

    protected val fragmentFactoryStore = FragmentFactoryStore().apply { setup() }
    protected var lastSavedInstanceState: Bundle? = null

    protected var onViewCreatedCount = 0
        private set

    protected var onCreatedCount = 0
        private set

    protected val isViewRecreated get() = onViewCreatedCount > 1
    protected val isFragmentRecreated get() = onCreatedCount > 1

    protected var binding: BINDING? = null
        private set

    fun requireBinding() = checkNotNull(binding)

    fun requireBinding(setup: BINDING.() -> Unit) = requireBinding().let(setup)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        childFragmentManager.fragmentFactory = CoreFragmentFactory(fragmentFactoryStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastSavedInstanceState = savedInstanceState
        onCreatedCount = savedInstanceState.getOr(SavedStateArgument.Int.ON_CREATED_COUNT) { 0 }
        onViewCreatedCount = savedInstanceState.getOr(SavedStateArgument.Int.ON_VIEW_CREATED_COUNT) { 0 }
        onCreatedCount++
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflateBinding(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition(200, TimeUnit.MILLISECONDS)
        onViewCreatedCount++
        checkNotNull(binding).onCreated()
    }

    override fun onSaveInstanceState(outState: Bundle) = with(outState) {
        super.onSaveInstanceState(this)
        putInt(SavedStateArgument.Int.ON_CREATED_COUNT, onCreatedCount)
        putInt(SavedStateArgument.Int.ON_VIEW_CREATED_COUNT, onViewCreatedCount)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    open fun BINDING.onCreated() = Unit

    open fun FragmentFactoryStore.setup() = Unit

    private object SavedStateArgument {
        object Int {
            const val ON_VIEW_CREATED_COUNT = "ON_VIEW_CREATED_COUNT"
            const val ON_CREATED_COUNT = "ON_CREATED_COUNT"
        }
    }
}