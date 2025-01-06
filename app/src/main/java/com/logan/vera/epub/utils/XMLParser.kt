package com.logan.vera.epub.utils

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

fun parseXMLFile(data: ByteArray): Element? {
    return try {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val document: Document = builder.parse(ByteArrayInputStream(data))
        document.documentElement
    } catch (e: Exception) {
        null
    }
}

fun Element.selectFirstTag(tagName: String): Element? =
    getElementsByTagName(tagName).item(0) as? Element

fun Element.selectChildTag(tagName: String): List<Element> =
    getElementsByTagName(tagName).run {
        (0 until length).mapNotNull { item(it) as? Element }
    }

fun Element.selectFirstChildTag(tagName: String): Element? =
    selectChildTag(tagName).firstOrNull()

fun Node.getAttribute(name: String): String =
    attributes?.getNamedItem(name)?.textContent ?: ""

fun Element.getAttributeValue(name: String): String = getAttribute(name)
