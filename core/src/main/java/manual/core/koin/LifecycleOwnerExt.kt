package manual.core.koin

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module

fun LifecycleOwner.attachKoinModule(moduleDeclaration: ModuleDeclaration) {
    val module = module { moduleDeclaration() }

    loadKoinModules(module)

    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                unloadKoinModules(module)
            }
        }
    )
}