package lib

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.ColorBuffer
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class OpenRouterAgent(
    val apiKey: String,
    val model: String = "google/gemma-3-12b-it:free"

) : Extension {
    override var enabled: Boolean = true

    val openai = OpenAI(
     logging = LoggingConfig(LogLevel.None),
        token = apiKey,
        timeout = Timeout(socket = 60.seconds),
        host = OpenAIHost(baseUrl = "https://openrouter.ai/api/v1/"),
        httpClientConfig = {
            this.engine {
            }
        }
    )

    val history: MutableList<ChatMessage> = mutableListOf()

    fun clearHistory() {
        history.clear()
    }

    fun setSystemPrompt(prompt: String) {
        history.add(0, ChatMessage(role = ChatRole.System, content = prompt))
    }

    fun chat(prompt: String): String {
        history.add(ChatMessage(role = ChatRole.User, content = prompt))
        return pushHistory()

    }

    fun chatColor(prompt: String): ColorRGBa {
        history.add(ChatMessage(role = ChatRole.User, content = "$prompt. Output a hex color code only, like #ffc0cb"))
        val color = pushHistory()
        val pcolor = Regex("#[0-9a-fA-F]{6}").find(color)?.value
        return if (pcolor != null) {
            rgb(pcolor)
        } else {
            ColorRGBa.WHITE
        }
    }

    fun chatContour(prompt: String): ShapeContour {
        history.add(ChatMessage(role = ChatRole.User, content = "$prompt. Output as an svg polyline and output an polyline element with a points attribute only"))
        val response = pushHistory()
        val polyline = Regex("<polyline .*/>").find(response)?.value

        if (polyline == null) {
            logger.warn { "no polyline found in response: '$response'" }
            return ShapeContour.EMPTY
        } else {
            val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"> $polyline </svg>"
            val compo = loadSVG(svg)
            val r = compo.findShapes().firstOrNull()?.shape?.contours?.firstOrNull() ?: ShapeContour.EMPTY
            if (r.empty) {
                logger.warn { "empty contour found in response: '$response'" }
            }
            return r
        }
    }

    fun chatColors(prompt: String): List<ColorRGBa> {
        history.add(
            ChatMessage(
                role = ChatRole.User,
                content = "$prompt. Output a list of comma separated hex color code only, like #ffc0cb, #ff0000, #abc123"
            )
        )
        val color = pushHistory()
        val pcolors = Regex("#[0-9a-fA-F]{6}").findAll(color)

        return pcolors.map { rgb(it.value) }.toList()
    }

    fun chatRectangle(prompt: String): Rectangle {
        history.add(
            ChatMessage(
                role = ChatRole.User,
                content = "$prompt. Output x and y coordinates of top-left and bottom-right separated by a comma, do not use additional formatting or text"
            )
        )
        val scoords = pushHistory()
        val ints = scoords.split(",").map { it.trim() }
        if (ints.size != 4) {
            logger.warn { "invalid coordinates: '$scoords'" }
            return Rectangle.EMPTY
        } else {
            val a = Vector2(ints[0].toDouble(), ints[1].toDouble())
            val b = Vector2(ints[2].toDouble(), ints[3].toDouble())
            return Rectangle(a.x, a.y, b.x - a.x, b.y - a.y)
        }
    }

    fun chatVector2(prompt: String): Vector2 {
        history.add(
            ChatMessage(
                role = ChatRole.User,
                content = "$prompt. Output x and y coordinates separated by a comma, do not use additional formatting or text"
            )
        )
        val scoords = pushHistory()
        val coords = scoords.split(",").map { it.trim().toIntOrNull() }.filterNotNull()
        if (coords.size == 2) {
            return coords.let { (x, y) -> Vector2(x.toDouble(), y.toDouble()) }
        } else {
            logger.warn { "invalid coordinates: '$scoords'" }
            return Vector2.INFINITY
        }
    }
    fun chatVector2s(prompt: String): List<Vector2> {
        history.add(
            ChatMessage(
                role = ChatRole.User,
                content = "$prompt. Output a list of coordinates separated by a comma, do not use additional formatting or tex, do not use parentheses"
            )
        )
        val scoords = pushHistory()
        val coords = scoords.split(",").map { it.trim().replace("(","").replace(")","").toIntOrNull() }.filterNotNull()
        if (coords.size.mod(2) == 0) {
            logger.info { "found ${coords.size/2} coordinates in '$scoords'" }
            return coords.windowed(2,2, false).map {
                Vector2(it[0].toDouble(), it[1].toDouble())
            }

        } else {
            logger.warn { "invalid coordinates: '$scoords'" }
            return emptyList()
        }

    }

    fun chatInt(prompt: String): Int {
        history.add(ChatMessage(role = ChatRole.User, content = "$prompt. Output an integer number only."))
        val color = pushHistory()
        val pcolor = Regex("[0-9]+").find(color)?.value
        return if (pcolor != null) {
            pcolor.toIntOrNull() ?: 0
        } else {
            0
        }
    }

    fun chatBoolean(prompt: String): Boolean {
        history.add(ChatMessage(role = ChatRole.User, content = "$prompt. Answer with yes or no only."))
        val yesOrNo = pushHistory().trim()

        if (yesOrNo.contains("yes", ignoreCase = true)) {
            return true
        } else if (yesOrNo.contains("no", ignoreCase = true)) {
            return false
        } else {
            return false
        }
    }

    private fun pushHistory(): String {

        val content = history.map { it.toString() }.joinToString("")
        val h = hash(content)
        val cacheDir = File("cache/openai")
        cacheDir.mkdirs()

        val cacheFile = File(cacheDir, "$h.json")

        if (cacheFile.exists()) {
            val message = Json.decodeFromString<ChatMessage>(cacheFile.readText())
            history.add(message)
            return message.content ?: ""
        }

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = history
        )
        return runBlocking {
            val completion: ChatCompletion = openai.chatCompletion(chatCompletionRequest)
            history.add(completion.choices.last().message)
            cacheFile.writeText(Json.encodeToString(completion.choices.last().message))
            completion.choices.last().message.content ?: ""
        }
    }

    fun imageChat(image: ColorBuffer, prompt: String): String {
        history.add(
            ChatMessage(
                role = ChatRole.User, content = listOf(
                    ImagePart(url = image.toDataUrl()),
                    TextPart("$prompt")
                )
            )
        )

        return pushHistory()
    }

    override fun shutdown(program: Program) {
        openai.close()
    }
}

fun Rectangle.map(before: Rectangle, after: Rectangle): Rectangle {
    val a = corner.map(before, after)
    val b = position(1.0, 1.0).map(before, after)
    return Rectangle(a.x, a.y, b.x - a.x, b.y - a.y)
}

fun ShapeContour.map(before: Rectangle, after: Rectangle): ShapeContour {
    return ShapeContour.fromSegments(this.segments.map {
        when (it.type) {
            SegmentType.LINEAR -> {
                Segment2D(
                    it.start.map(before, after),
                    it.end.map(before, after)
                )
            }
            SegmentType.QUADRATIC -> {
                Segment2D(
                    it.start.map(before, after),
                    it.control[0].map(before, after),
                    it.end.map(before, after)
                )
            }
            SegmentType.CUBIC -> {
                Segment2D(
                    it.start.map(before, after),
                    it.control[0].map(before, after),
                    it.control[1].map(before, after),
                    it.end.map(before, after)
                )
            }
        }


    }, closed = closed)
}