package examples

import lib.OpenRouterAgent
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.noise.phrases.fhash12
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.textwriter.writer
import org.openrndr.math.Vector2

fun main() {
    application {
        configure {
            width = 720
            height = 960
        }
        program {
            val ai =
                extend(OpenRouterAgent("your_api_key_here"))

            val image = loadImage("data/images/cheeta.jpg")
            val imageDescription = ai.imageChat(image, "Write an 8 sentence fairy tale describing the colors in the image. Don't use lists or text formatting. Don't mention the image itself only what is on the image.")
            val colors8 = ai.chatColors("What are the eight most dominant colors in the image?")

            ai.clearHistory()

            extend(Screenshots())
            extend(Camera2D())
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.shadeStyle = gradient<ColorRGBa> {
                    for (i in 0 until colors8.size) {
                        stops[i/(colors8.size-1.0)] = colors8[i]
                    }
                    linear {
                        start = Vector2(0.0, 0.0)
                        end = Vector2(1.0, 1.0)

                    }
                    levelWarpFunction = """
                        $fhash12
                        float levelWarp(vec2 uv, float l) { return l + fhash12(uv)*0.1; }"""

                }
//                val (m0, m1) =
//                    drawer.imageFit(image, drawer.bounds.offsetEdges(-10.0))

                drawer.rectangle(drawer.bounds)

                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = ColorRGBa.WHITE

                drawer.shadeStyle = null

                drawer.fontMap = loadFont("data/fonts/default.otf", 32.0)
                writer {
                    box = drawer.bounds.offsetEdges(-30.0)
                    verticalAlign = 0.5
                    horizontalAlign = 0.5
                    text(imageDescription)
                }

            }
        }
    }
}