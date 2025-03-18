package examples

import lib.OpenRouterAgent
import lib.map
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.colormatrix.colorMatrix
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.phrases.fhash12
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.textwriter.writer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.map

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
            val ascii = ai.imageChat(image, "Convert the image to ascii art, make it 40 columns wide and 20 rows high. Output the ascii-art only.")

            ai.clearHistory()

            extend(Screenshots())
            extend(Camera2D())
            extend {
                drawer.clear(ColorRGBa.BLACK)

                drawer.fontMap = loadFont("data/fonts/default.otf", 32.0)
                writer {
                    verticalAlign = 0.5
                    text(ascii)
                }
            }
        }
    }
}