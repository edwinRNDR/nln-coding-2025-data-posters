package lib

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Serializable
class DuckDuckGoResult(
    var image: String,
    var image_token: String,
    var height: Int,
    var thumbnail: String,
    var thumbnail_token: String,
    var source: String,
    var width: Int,
    var url: String,
    var title: String
) {
    @Contextual
    var imageSmall: ColorBuffer? = null
}

@Serializable
class SearchResults(
    var ads: String?,
    var response_type: String,
    var results: List<DuckDuckGoResult>,
    var query: String,
    var queryEncoded: String,
    var next: String,
    var vqd: Map<String, String>
)

private fun encodeValue(value: String): String {
    try {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    } catch (ex: UnsupportedEncodingException) {
        throw RuntimeException(ex.cause)
    }

}

fun searchImages(query: String): SearchResults {


    val hash = hash(query)

    val cacheDir = File("cache")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    val targetFile = File("cache/ddg_$hash.json")

    if (targetFile.exists()) {
        val json = targetFile.readText()
        return Json.decodeFromString<SearchResults>(json)
    } else {
        val safeQuery = encodeValue(query)
        val pageUrl = "https://duckduckgo.com/?q=$safeQuery&t=h_&iar=images"

        val page = URL(pageUrl)
        val pageCon = page.openConnection()
        pageCon.setRequestProperty("authority", "duckduckgo.com")
        pageCon.setRequestProperty("cache-control", "max-age=0")
        pageCon.setRequestProperty("upgrade-insecure-requests", "1")
        pageCon.setRequestProperty(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
        )
        pageCon.setRequestProperty(
            "accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
        )
        pageCon.connect()
        val result = pageCon.getInputStream().reader().readText()


        val r = Regex("vqd=\"([0-9-]*)\"", RegexOption.MULTILINE)
        val m = r.find(result)
        val vqd = m?.groupValues!![1]


        //https://duckduckgo.com/i.js?q=mc%20hammer&o=json&p=1&s=100&u=bing&f=,,,,,&l=us-en&vqd=4-114601115750128188721913283243174227316
        //curl 'https://duckduckgo.com/?q=prince&t=h_&iax=images&ia=images'
        // -H 'authority: duckduckgo.com'
        // -H 'cache-control: max-age=0'
        // -H 'upgrade-insecure-requests: 1'
        // -H 'user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36'
        // -H 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3' -H 'accept-encoding: gzip, deflate, br'
        // -H 'accept-language: en-US,en;q=0.9,nl;q=0.8' --compressed
//    println(page)

        val baseUrl = "https://duckduckgo.com/i.js?q=$safeQuery&o=json&p=1&s=100&u=bing&f=,,,,,&l=us-en&vqd=$vqd"
        val base = URL(baseUrl)
        val baseCon = base.openConnection()
        baseCon.setRequestProperty("accept-language", "en-US,en;q=0.9,nl;q=0.8")
        baseCon.setRequestProperty(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
        )
        baseCon.setRequestProperty("referer", "https://duckduckgo.com")
        baseCon.setRequestProperty("authority", "duckduckgo.com")
        baseCon.setRequestProperty("x-requested-with", "XMLHttpRequest")
        baseCon.connect()
        val baseResult = baseCon.getInputStream().reader().readText()
        targetFile.writeText(baseResult)

        val realResult = Json.decodeFromString<SearchResults>(baseResult)
        return realResult
    }

}

fun scrapeImages(query: String, maxImages:Int = 1000): List<DuckDuckGoResult> {
    val r = searchImages(query)

    val toProc = r.results.take(maxImages)

    for ((i,result) in toProc.withIndex()) {
        try {
            println("downloading image [${i+1}/${toProc.size}]")
            result.imageSmall = loadImage(result.thumbnail)
        } catch (e:Exception) {
            println("error downloading ${result.thumbnail}")
            result.imageSmall = colorBuffer(256,256)
        }
    }
    return toProc
}

fun duckDuckGoSequence(query:String) = sequence {
    while (true) {
        for (result in searchImages(query).results) {
            val fa = FileArticle(result.title, listOf(result.title), listOf(result.thumbnail))
            fa.file()
            yield(fa)
        }
    }
}

