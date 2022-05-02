package manual.core.resources

import android.content.res.AssetManager
import android.util.Log
import java.io.InputStream

fun AssetManager.forEachFilePath(
    rootPath: String,
    action: (filePath: String) -> Unit
) {
    runCatching {
        val list = checkNotNull(list(rootPath))
        if (list.isEmpty()) {
            action(rootPath)
        } else {
            list.forEach {
                forEachFilePath("$rootPath/$it", action)
            }
        }
    }.onFailure {
        Log.e("AssetManager", it.stackTraceToString())
    }
}

fun <T> AssetManager.mapEachFilePath(
    rootPath: String,
    transform: AssetManager.(filePath: String) -> T
): List<T> = mutableListOf<T>().apply {
    forEachFilePath(rootPath) {
        add(transform(it))
    }
}

fun <T> AssetManager.mapEachFile(
    rootPath: String,
    transform: AssetManager.(data: String) -> T
) = mapEachFilePath(rootPath) {
    transform(read(it))
}

fun AssetManager.forEachInputStream(
    rootPath: String,
    action: (inputStream: InputStream) -> Unit
) = forEachFilePath(rootPath) {
    action(open(it))
}

fun <T> AssetManager.mapEachInputStream(
    rootPath: String,
    transform: AssetManager.(inputStream: InputStream) -> T
): List<T> = mutableListOf<T>().apply {
    forEachInputStream(rootPath) {
        add(transform(it))
    }
}

fun AssetManager.read(path: String) = open(path).use {
    it.readBytes().decodeToString()
}