package examples.csv

import lib.LoadedArticle
import lib.csvArchive
import org.openrndr.application
import org.openrndr.draw.Session
import org.openrndr.draw.loadFont
import org.openrndr.extra.compositor.Composite
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.textwriter.writer
import org.openrndr.launch

fun main() = application {
    configure {
        width = 600
        height = 800
    }
    program {
        val archive = csvArchive("archives/dutch-news/data.csv").iterator()
        var article = archive.next()

        fun makePoster(article: LoadedArticle): Composite =
            compose {
                if (article.images.isNotEmpty()) {
                    layer {
                        draw {
                            drawer.imageFit(article.images[0], drawer.bounds.offsetEdges(-10.0))
                        }
                        post(ApproximateGaussianBlur()) {
                            window = 10
                            sigma = 10.0
                        }
                    }
                }
                layer {
                    draw {
                        drawer.fontMap = loadFont("data/fonts/default.otf", 64.0)
                        writer {
                            box = drawer.bounds.offsetEdges(-100.0)
                            horizontalAlign = 0.5
                            verticalAlign = 0.5
                            text(article.title)
                        }
                    }
                }
            }

        var composite = makePoster(article)

        extend {
            composite.draw(drawer)
        }

        //<editor-fold desc="complex data loading code, ignore me">
        var session: Session? = null
        keyboard.character.listen {
            if (it.character == 'n') {
                launch {
                    session?.end()
                    session = Session.active.fork()

                    val next =
                        archive.next()

                    article.destroy()
                    article = next
                    composite = makePoster(article)
                }
            }
        }
        //</editor-fold>
    }
}