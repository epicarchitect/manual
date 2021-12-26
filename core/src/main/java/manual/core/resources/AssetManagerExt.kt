package manual.core.resources

import android.content.res.AssetManager

fun AssetManager.forEachFilePath(
    rootPath: String,
    action: (filePath: String) -> Unit
) {
    val list = checkNotNull(list(rootPath))
    if (list.isEmpty()) {
        action(rootPath)
    } else {
        list.forEach {
            forEachFilePath("$rootPath/$it", action)
        }
    }
}

fun <T> AssetManager.mapEachFilePath(
    rootPath: String,
    transform: AssetManager.(filePath: String) -> T
) = mutableListOf<T>().apply {
    forEachFilePath(rootPath) {
        add(transform(it))
    }
}.toList()

fun <T> AssetManager.mapEachFile(
    rootPath: String,
    transform: AssetManager.(data: String) -> T
) = mapEachFilePath(rootPath) {
    transform(read(it))
}

fun AssetManager.read(path: String) = open(path).use {
    it.readBytes().decodeToString()
}