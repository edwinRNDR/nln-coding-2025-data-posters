package lib

import io.github.oshai.kotlinlogging.KotlinLogging


import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.openrndr.draw.loadImage
import java.io.File

private val logger = KotlinLogging.logger {}


fun csvArchive(
    csvFile: String,
    imageDir: String = "images",
    imageColumns: List<String> = listOf("image"),
    titleColumn: String = "title",
    bodyColumn: String = "body"
): Sequence<LoadedArticle> {

    val csv = File(csvFile)
    val baseDir = csv.parentFile
    val imageDir = File(baseDir, "images")
    require(baseDir.isDirectory)

    var current: LoadedArticle? = null
    return sequence {

        while (true) {
            val rows = csvReader().open(csv) { readAllWithHeaderAsSequence().toList() }
            for (row in rows) {

                val images = imageColumns.mapNotNull {
                    try {
                        loadImage(File(imageDir, row[it]?:"empty"))
                    } catch (e: Exception) {
                        logger.error(e) { "failed to load image for ${row[it]}: ${e.message}" }
                        null
                    }
                }

                current?.destroy()
                current = LoadedArticle(
                    row[titleColumn] ?: "",
                    listOf(row[bodyColumn] ?: ""),
                    images,
                    fields = row
                )
                yield(current!!)

            }
        }

    }
}
