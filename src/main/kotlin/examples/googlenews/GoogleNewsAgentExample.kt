package examples

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import lib.*
import org.openrndr.application
import org.openrndr.draw.Session
import org.openrndr.draw.loadFont
import org.openrndr.extra.compositor.*
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.textwriter.writer
import org.openrndr.launch


fun main() = application {
    configure {
        width = 600
        height = 800
    }
    program {
        val ai =
            extend(OpenRouterAgent("your_api_key_here"))

        googleNewsSetApiKey("your_google_news_api_key_here")
        val archive = googleNewsSequence(GoogleNewsEndPoint.TopHeadlines, category = "general").iterator()
        //val archive = googleNewsSequence(GoogleNewsEndPoint.Everything, query = "tariff").iterator()
        var article = archive.next().load()
        
        fun makePoster(article: LoadedArticle): Composite =
            compose {
                ai.clearHistory()

                println(article)
                if (article.images.isNotEmpty()) {

                    val imageDescription = ai.imageChat(article.images[0], "Give a single sentence description of the image.")

                    layer {
                        draw {
                            drawer.imageFit(article.images[0], drawer.bounds.offsetEdges(-10.0))
                            drawer.fontMap = loadFont("data/fonts/default.otf", 24.0)

                            writer {
                                box = drawer.bounds.offsetEdges(-40.0)
                                horizontalAlign = 0.5
                                verticalAlign = 1.0
                                if (imageDescription.isNotEmpty()) {
                                    text(imageDescription)
                                }
                            }
                        }
//                        post(ApproximateGaussianBlur()) {
//                            window = 10
//                            sigma = 10.0
//                        }
                    }
                }

                layer {

                    draw {
                        drawer.fontMap = loadFont("data/fonts/default.otf", 64.0)
                        writer {
                            box = drawer.bounds.offsetEdges(-50.0)
                            horizontalAlign = 0.5
                            verticalAlign = 0.0
                            text(article.title)
                        }

                        drawer.fontMap = loadFont("data/fonts/default.otf", 24.0)
                        writer {
                            box = drawer.bounds.offsetEdges(-10.0)
                            horizontalAlign = 0.5
                            verticalAlign = 1.0
                            text(article.date?:" ")
                        }

                        writer {
                            box = drawer.bounds.offsetEdges(-10.0)
                            horizontalAlign = 0.5
                            verticalAlign = 0.0
                            text(article.source?:" ")
                        }
                    }
                }
            }

        var composite = makePoster(article)

        var session: Session? = null
        keyboard.character.listen {
            if (it.character == 'n') {
                val next = GlobalScope.async {
                    archive.next()
                }
                launch {
                    val newArticle = next.await().load()
                    article.destroy()
                    article = newArticle
                    session?.end()
                    session = Session.active.fork()
                    composite = makePoster(article)
                }
            }
        }

        extend {
            composite.draw(drawer)
        }
    }
}