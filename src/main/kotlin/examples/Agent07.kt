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
            val imageDescription = ai.imageChat(image, "Write an 1 sentence description of the image. Don't use lists or text formatting. Don't mention the image itself only what is on the image.")
            val contour = ai.chatContour("Extract the contour of the subject in the image.")

            ai.clearHistory()

            extend(Screenshots())
            extend(Camera2D())
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.imageFit(image, drawer.bounds.offsetEdges(-10.0))
                drawer.fill = null
                drawer.stroke = ColorRGBa.RED


                drawer.contour(contour)
            }
        }
    }
}