package examples

import lib.OpenRouterAgent
import lib.map
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.primitives.grid
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
            val imageDescription = ai.imageChat(image, "Describe the image in a single sentence.")
            val boundingBox = ai.chatRectangle("Give the bounding box of the head of the subject in the image. Output x and y coordinates separated by a comma, do not use additional formatting or text")
            val colors = ai.chatColors("What are the four most dominant colors in the image?")

            val count = ai.chatInt("How many animals are in the image?")
            val outline = ai.chatContour("What is the outline of the subject in the image?")

            ai.clearHistory()

            val heart = ai.chatContour("Draw a 300 px wide heart, the center of the heart should be at the coordinates (${width/2}, ${height/2}).")

            println(outline)
            extend(Camera2D())
            extend {

                drawer.clear(colors[0])
                val (m0, m1) = drawer.imageFit(image, drawer.bounds.offsetEdges(-10.0))
                //drawer.circle(coords.map(m0, m1), 10.0)

                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE

                drawer.rectangle(boundingBox.map(m0, m1))
                drawer.contour(outline.map(m0, m1))
                drawer.stroke = ColorRGBa.RED
                drawer.contour(heart)
                drawer.fontMap = loadFont("data/fonts/default.otf", 32.0)
                val grid = drawer.bounds.grid(4,1, 10.0, 10.0, 10.0, 10.0).flatten()

                for ((index, c) in colors.withIndex()) {
                    drawer.fill = c
//                    drawer.rectangle(grid[index])
                }
                writer {
                    box = drawer.bounds.offsetEdges(-100.0)
                    verticalAlign = 0.5
                    horizontalAlign = 0.5
                    text(imageDescription)
                }

                drawer.text(count.toString(), width/2.0, 20.0)
            }
        }
    }
}