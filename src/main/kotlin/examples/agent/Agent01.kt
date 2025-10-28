package examples

import lib.OpenRouterAgent
import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.textwriter.writer

fun main() {
    application {
        program {
            val ai =
                extend(OpenRouterAgent("your_api_key_here"))

            val image = loadImage("data/images/cheeta.jpg")

            val imageDescription = ai.imageChat(image, "Describe the image in a single sentence.")
            println(imageDescription)
            val love = ai.chat("Explain biodiversity in a single sentence. Don't use lists or text formatting. Don't output click here texts").replace("click here.", "\n", ignoreCase = true)
            val color = ai.chatColor("What is the color of the concept you just explained?")
            println(love)
            println(color)
            extend {

                drawer.fontMap = loadFont("data/fonts/default.otf", 32.0)
                drawer.fill = color
                writer {
                    box = drawer.bounds.offsetEdges(-100.0)
                    verticalAlign = 0.5
                    horizontalAlign = 0.5
                    text(love)
                }
            }
        }
    }
}