package examples

import lib.OpenRouterAgent
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.colormatrix.colorMatrix
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.textwriter.writer

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
            val imageDescription = ai.imageChat(image, "Write an 8 sentence fairy tale describing the image. Don't use lists or text formatting. Don't mention the image itself only what is on the image.")

            ai.clearHistory()

            extend(Screenshots())
            extend(Camera2D())
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.drawStyle.colorMatrix = colorMatrix {
                    tint(ColorRGBa.WHITE.shade(0.25))
                    grayscale()
                }
                val (m0, m1) = drawer.imageFit(image, drawer.bounds.offsetEdges(-10.0))

                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = ColorRGBa.WHITE

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