package manual.app.parser

import android.util.Log
import android.util.Xml
import manual.app.data.Chapter
import manual.app.data.Content
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class ChapterParser {

    fun parse(inputStream: InputStream) = with(Xml.newPullParser()) {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(inputStream, null)
        nextTag()
        readChapter()
    }

    private fun XmlPullParser.readChapter(): Chapter {
        var chapterId: Int? = null
        var chapterName: String? = null
        val chapterContents = mutableListOf<Content>()

        repeat(attributeCount) {
            when (val attributeName = getAttributeName(it)) {
                ATTRIBUTE_CHAPTER_ID -> chapterId = getAttributeValue(it).toInt()
                ATTRIBUTE_CHAPTER_NAME -> chapterName = getAttributeValue(it)
                else -> error("Unexpected chapter attribute: $attributeName.")
            }
        }

        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) {
                continue
            }

            chapterContents.add(
                when (name) {
                    TAG_TEXT -> readTextTag()
                    TAG_AUDIO -> readAudioTag().also { nextTag() }
                    TAG_VIDEO -> readVideoTag().also { nextTag() }
                    TAG_GIF -> readGifTag().also { nextTag() }
                    TAG_IMAGE -> readImageTag().also { nextTag() }
                    else -> error("Unexpected chapter tag: $name")
                }
            )
        }

        return Chapter(
            checkNotNull(chapterId),
            checkNotNull(chapterName),
            chapterContents
        )
    }

    private fun XmlPullParser.readTextTag(): Content.Text{
        val stringBuilder = StringBuilder()
        val startDepth = depth

        while (next() != XmlPullParser.END_TAG || startDepth != depth) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    stringBuilder.append("<")
                    stringBuilder.append(name)
                    repeat(attributeCount) {
                        stringBuilder.append(" ")
                        stringBuilder.append(getAttributeName(it))
                        stringBuilder.append("=")
                        stringBuilder.append("\"")
                        stringBuilder.append(getAttributeValue(it))
                        stringBuilder.append("\"")
                    }
                    stringBuilder.append(">")
                }
                XmlPullParser.END_TAG -> {
                    stringBuilder.append("</")
                    stringBuilder.append(name)
                    stringBuilder.append(">")
                }
                XmlPullParser.TEXT -> {
                    stringBuilder.append(text)
                }
            }
        }

        Log.d("test123", stringBuilder.toString())
        return Content.Text(stringBuilder.toString())
    }

    private fun XmlPullParser.readAudioTag(): Content.Audio {
        var audioSrc: String? = null
        var audioName: String? = null

        repeat(attributeCount) {
            when (val attributeName = getAttributeName(it)) {
                ATTRIBUTE_AUDIO_SRC -> audioSrc = getAttributeValue(it)
                ATTRIBUTE_AUDIO_NAME -> audioName = getAttributeValue(it)
                else -> error("Unexpected audio attribute: $attributeName")
            }
        }

        return Content.Audio(
            checkNotNull(audioSrc),
            audioName
        )
    }

    private fun XmlPullParser.readVideoTag(): Content.Video {
        var videoSrc: String? = null
        var videoName: String? = null

        repeat(attributeCount) {
            when (val attributeName = getAttributeName(it)) {
                ATTRIBUTE_VIDEO_SRC -> videoSrc = getAttributeValue(it)
                ATTRIBUTE_VIDEO_NAME -> videoName = getAttributeValue(it)
                else -> error("Unexpected video attribute: $attributeName")
            }
        }

        return Content.Video(
            checkNotNull(videoSrc),
            videoName
        )
    }

    private fun XmlPullParser.readGifTag(): Content.Gif {
        var gifSrc: String? = null
        var gifName: String? = null

        repeat(attributeCount) {
            when (val attributeName = getAttributeName(it)) {
                ATTRIBUTE_GIF_SRC -> gifSrc = getAttributeValue(it)
                ATTRIBUTE_GIF_NAME -> gifName = getAttributeValue(it)
                else -> error("Unexpected gif attribute: $attributeName")
            }
        }

        return Content.Gif(
            checkNotNull(gifSrc),
            gifName
        )
    }

    private fun XmlPullParser.readImageTag(): Content.Image {
        var imageSrc: String? = null
        var imageName: String? = null

        repeat(attributeCount) {
            when (val attributeName = getAttributeName(it)) {
                ATTRIBUTE_IMAGE_SRC -> imageSrc = getAttributeValue(it)
                ATTRIBUTE_IMAGE_NAME -> imageName = getAttributeValue(it)
                else -> error("Unexpected image attribute: $attributeName")
            }
        }

        return Content.Image(
            checkNotNull(imageSrc),
            imageName
        )
    }

    companion object {
        /** Chapter */
        private const val ATTRIBUTE_CHAPTER_ID = "id"
        private const val ATTRIBUTE_CHAPTER_NAME = "name"

        /** Text */
        private const val TAG_TEXT = "text"

        /** Audio */
        private const val TAG_AUDIO = "audio"
        private const val ATTRIBUTE_AUDIO_SRC = "src"
        private const val ATTRIBUTE_AUDIO_NAME = "name"

        /** Video */
        private const val TAG_VIDEO = "video"
        private const val ATTRIBUTE_VIDEO_SRC = "src"
        private const val ATTRIBUTE_VIDEO_NAME = "name"

        /** Gif */
        private const val TAG_GIF = "gif"
        private const val ATTRIBUTE_GIF_SRC = "src"
        private const val ATTRIBUTE_GIF_NAME = "name"

        /** Image */
        private const val TAG_IMAGE = "image"
        private const val ATTRIBUTE_IMAGE_SRC = "src"
        private const val ATTRIBUTE_IMAGE_NAME = "name"
    }
}