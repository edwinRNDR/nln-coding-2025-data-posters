package lib

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.draw.persistent
import org.openrndr.draw.probeImage
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

class FileArticle(
    val title: String, val texts: List<String>, val imageUrls: List<String>,
    val date: String? = null,
    val source: String? = null,
    val author: String? = null
) {
    fun file() {
        for (iu in imageUrls) {
            val imageCache = File("cache/images")
            if (!imageCache.exists()) {
                imageCache.mkdirs()
            }
            downloadFile(iu, File(imageCache, hashImageUrl(iu)))
        }
    }

    fun load(): LoadedArticle {
        val imageCache = File("cache/images")
        val images = imageUrls.sorted().mapNotNull {
            val hash = File(imageCache, hashImageUrl(it))
            if (hash.exists() && probeImage(hash) != null) {
                persistent { loadImage(hash) }
            } else null
        }
        return LoadedArticle(title, texts, images, date, source, author)
    }
}

fun downloadFile(urlString: String, target: File) {
    if (!target.exists()) {
        try {
            val data = URL(urlString).readBytes()
            target.writeBytes(data)
        } catch (e: FileNotFoundException) {
            println("file not found: ${urlString}")
        } catch(e: IOException) {
            println("io error (${e.message}: ${urlString}")
        }
    }
}

data class LoadedArticle(
    val title: String,
    val texts: List<String>,
    val images: List<ColorBuffer>,
    val date: String? = null,
    val source: String? = null,
    val author: String? = null,
    val fields: Map<String, String> = emptyMap()
) {
    fun destroy() {
        images.forEach { it.destroy() }
    }

//    val imageStatistics by lazy {
//        images.map { it.statistics() }
//    }

}

fun hashImageUrl(url: String): String {
    val extension = when {
        url.endsWith(".png") -> ".png"
        url.endsWith(".jpg") -> ".jpg"
        else -> ".jpg" // -- yeah about that ..
    }
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(url.toByteArray())
    return digest.fold("", { str, it -> str + "%02x".format(it) }) + extension
}