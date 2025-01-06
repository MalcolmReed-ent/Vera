package com.logan.vera.epub.parser

import android.graphics.BitmapFactory
import com.logan.vera.epub.utils.BookTextMapper
import com.logan.vera.epub.utils.EpubFile
import com.logan.vera.epub.utils.decodedURL
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.File
import kotlin.io.path.invariantSeparatorsPathString

class EpubXMLParser(
    private val fileAbsolutePath: String,
    private val data: ByteArray,
    private val zipFile: Map<String, EpubFile>
) {
    data class Output(
        val title: String?,
        val body: String
    )

    private val fileParentFolder: File = File(fileAbsolutePath).parentFile ?: File("")

    fun parseAsDocument(): Output {
        val body = Jsoup.parse(data.inputStream(), "UTF-8", "").body()
        
        // Extract title from headers if present
        val title = body.selectFirst("h1, h2, h3, h4, h5, h6")?.text()
        body.selectFirst("h1, h2, h3, h4, h5, h6")?.remove()

        return Output(
            title = title,
            body = getNodeStructuredText(body)
        )
    }

    fun parseAsImage(absolutePathImage: String): String {
        val bitmap = zipFile[absolutePathImage]?.data?.runCatching {
            BitmapFactory.decodeByteArray(this, 0, this.size)
        }?.getOrNull()

        val text = BookTextMapper.ImgEntry(
            path = absolutePathImage,
            yrel = bitmap?.let { it.height.toFloat() / it.width.toFloat() } ?: 1.45f
        ).toXMLString()

        return "\n\n$text\n\n"
    }

    private fun getNodeStructuredText(node: Node): String {
        return when {
            node is TextNode -> handleTextNode(node)
            node is Element -> handleElement(node)
            else -> node.childNodes()
                .joinToString("") { getNodeStructuredText(it) }
        }
    }

    private fun handleTextNode(node: TextNode): String {
        val text = node.text().trim()
        return if (text.isEmpty()) "" else text
    }

    private fun handleElement(element: Element): String {
        return when (element.tagName()) {
            "p" -> handleParagraph(element)
            "br" -> "\n"
            "hr" -> "\n\n"
            "img", "image" -> handleImage(element)
            else -> element.childNodes()
                .joinToString("") { getNodeStructuredText(it) }
        }
    }

    private fun handleParagraph(paragraph: Element): String {
        val content = paragraph.childNodes()
            .joinToString("") { getNodeStructuredText(it) }
            .trim()
        return if (content.isEmpty()) "" else "$content\n\n"
    }

    private fun handleImage(imageElement: Element): String {
        val attrs = imageElement.attributes().associate { it.key to it.value }
        val relPathEncoded = attrs["src"] ?: attrs["xlink:href"] ?: return ""

        val absolutePathImage = File(fileParentFolder, relPathEncoded.decodedURL)
            .canonicalFile
            .toPath()
            .invariantSeparatorsPathString
            .removePrefix("/")

        // Check if image exists in zip file
        return if (zipFile.containsKey(absolutePathImage)) {
            parseAsImage(absolutePathImage)
        } else ""
    }

    companion object {
        private val BLOCK_ELEMENTS = setOf(
            "address", "article", "aside", "blockquote", "canvas", "dd", "div",
            "dl", "dt", "fieldset", "figcaption", "figure", "footer", "form",
            "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "li", "main",
            "nav", "noscript", "ol", "p", "pre", "section", "table", "tfoot",
            "ul", "video"
        )

        fun isBlockElement(element: Element): Boolean =
            BLOCK_ELEMENTS.contains(element.tagName().lowercase())
    }
}
