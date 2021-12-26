package manual.core.os

import android.os.Bundle

inline fun <reified T> Bundle?.getOr(key: String, function: () -> T): T {
    if (this == null) return function()

    return if (containsKey(key)) {
        get(key) as T
    } else {
        function()
    }
}

inline fun <reified T> Bundle?.require(key: String) = getOr<T>(key) { error("Key $key not found.") }