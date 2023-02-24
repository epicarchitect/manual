package manual.core.fragment

import androidx.fragment.app.FragmentFactory

class CoreFragmentFactory(private val fragmentFactoryStore: FragmentFactoryStore) :
    FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String) =
        fragmentFactoryStore.getFactory(className)?.invoke()
            ?: super.instantiate(classLoader, className)
}