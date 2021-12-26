@file:Suppress("UNCHECKED_CAST")

package manual.core.fragment

import androidx.fragment.app.Fragment

class FragmentFactoryStore(setup: FragmentFactoryStore.() -> Unit = {}) {

    private val factories = HashMap<String, () -> Fragment>()

    init {
        setup()
    }

    fun getFactory(className: String) = factories[className]

    fun setFactory(className: String, function: () -> Fragment) {
        factories[className] = function
    }
}

inline fun <reified F : Fragment> FragmentFactoryStore.instantiate(setup: F.() -> Unit = { }) =
    getFactory<F>().invoke().apply(setup)

inline fun <reified F : Fragment> FragmentFactoryStore.setFactory(noinline function: () -> F) =
    setFactory(F::class.java.name, function)

inline fun <reified F : Fragment> FragmentFactoryStore.getFactory() =
    getFactory(F::class.java.name) as () -> F