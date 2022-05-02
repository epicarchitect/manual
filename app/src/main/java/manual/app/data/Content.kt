package manual.app.data

sealed class Content {
    data class Text(val value: String) : Content()
    data class Video(val source: String, val name: String?) : Content()
    data class Image(val source: String, val name: String?) : Content()
    data class Gif(val source: String, val name: String?) : Content()
    data class Audio(val source: String, val name: String?) : Content()
}