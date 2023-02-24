package manual.core.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import manual.core.R
import manual.core.fragment.CoreFragmentFactory
import manual.core.fragment.FragmentFactoryStore
import manual.core.os.getOr

abstract class CoreActivity<BINDING : ViewBinding>(
    private val inflateBinding: (LayoutInflater) -> BINDING
) : AppCompatActivity() {

    protected var lastIntent: Intent? = null
        private set

    protected val fragmentFactoryStore = FragmentFactoryStore().apply { setup() }

    protected var onCreatedCount = 0
        private set

    protected val isRecreated get() = onCreatedCount > 1

    protected var binding: BINDING? = null
        private set

    fun requireBinding() = checkNotNull(binding)

    fun requireBinding(setup: BINDING.() -> Unit) = requireBinding().let(setup)

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = CoreFragmentFactory(fragmentFactoryStore)
        onCreatedCount = savedInstanceState.getOr(SavedStateArgument.Int.ON_CREATED_COUNT) { 0 }
        onCreatedCount++

        lastIntent = savedInstanceState.getOr(SavedStateArgument.Parcelable.LAST_INTENT) {
            if (isRecreated) null else intent
        }

        setTheme(getThemeResourceId())
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater).also {
            setContentView(it.root)
            it.onCreated()
        }
    }

    override fun onResume() {
        super.onResume()
        if (tryConsumeIntent(lastIntent)) {
            lastIntent = null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        lastIntent = if (tryConsumeIntent(intent)) {
            null
        } else {
            intent
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SavedStateArgument.Int.ON_CREATED_COUNT, onCreatedCount)
        lastIntent?.let {
            outState.putParcelable(SavedStateArgument.Parcelable.LAST_INTENT, it)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    @StyleRes
    open fun getThemeResourceId() = R.style.Theme_MaterialComponents

    open fun tryConsumeIntent(intent: Intent?) = false

    open fun BINDING.onCreated() = Unit

    open fun FragmentFactoryStore.setup() = Unit

    private object SavedStateArgument {
        object Int {
            const val ON_CREATED_COUNT = "ON_CREATED_COUNT"
        }

        object Parcelable {
            const val LAST_INTENT = "LAST_INTENT"
        }
    }
}