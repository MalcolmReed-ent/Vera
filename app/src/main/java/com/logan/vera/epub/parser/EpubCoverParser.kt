package com.logan.vera.epub.parser

import com.logan.vera.epub.models.EpubBook.Image
import com.logan.vera.epub.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.InputStream
import kotlin.io.path.invariantSeparatorsPathString

class EpubCoverParser {
    companion object {
        @Throws(Exception::class)
        suspend fun parse(inputStream: InputStream): Image? = withContext(Dispatchers.Default) {
            val files = EpubParser.getZipFiles(inputStream)

            // Parse container.xml
            val container = files["META-INF/container.xml"]
                ?: throw Exception("META-INF/container.xml file missing")

            val opfFilePath = parseXMLFile(container.data)
                ?.selectFirstTag("rootfile")
                ?.getAttributeValue("full-path")
                ?.decodedURL ?: throw Exception("Invalid container.xml file")

            // Get OPF file
            val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

            // Parse OPF file
            val document = parseXMLFile(opfFile.data)
                ?: throw Exception(".opf file failed to parse data")
            
            val metadata = document.selectFirstTag("metadata")
                ?: throw Exception(".opf file metadata section missing")
            val manifest = document.selectFirstTag("manifest")
                ?: throw Exception(".opf file manifest section missing")
            val guide = document.selectFirstTag("guide")

            // Get cover ID from metadata
            val metadataCoverId = metadata
                .selectChildTag("meta")
                .find { it.getAttributeValue("name") == "cover" }
                ?.getAttributeValue("content")

            // Setup path resolution
            val hrefRootPath = File(opfFilePath).parentFile ?: File("")
            fun String.hrefAbsolutePath() = File(hrefRootPath, this).canonicalFile
                .toPath()
                .invariantSeparatorsPathString
                .removePrefix("/")

            // Parse manifest items
            val manifestItems = manifest
                .selectChildTag("item")
                .map {
                    ManifestItem(
                        id = it.getAttribute("id"),
                        absoluteFilePath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
                        mediaType = it.getAttribute("media-type"),
                        properties = it.getAttribute("properties")
                    )
                }.associateBy { it.id }

            // Try to get cover image from manifest
            var coverImage = manifestItems[metadataCoverId]
                ?.let { files[it.absoluteFilePath] }
                ?.let { Image(absPath = it.absPath, image = it.data) }

            // If no cover found, try alternative methods
            if (coverImage == null) {
                coverImage = findCoverImageAlternative(guide, manifestItems, files)
            }

            coverImage
        }

        private fun findCoverImageAlternative(
            guide: org.w3c.dom.Element?,
            manifestItems: Map<String, ManifestItem>,
            files: Map<String, EpubFile>
        ): Image? {
            // Try to find cover in guide
            val coverHref = guide?.selectChildTag("reference")
                ?.find { it.getAttribute("type") == "cover" }
                ?.getAttributeValue("href")
                ?.decodedURL

            // If guide doesn't have cover, try manifest
            val manifestCoverItem = if (coverHref == null) {
                manifestItems["cover"]
            } else null

            val coverPath = coverHref ?: manifestCoverItem?.absoluteFilePath
            val coverFile = coverPath?.let { files[it] }

            return if (coverFile != null) {
                if (coverFile.absPath.endsWith(".html", true) || 
                    coverFile.absPath.endsWith(".xhtml", true)) {
                    parseCoverImageFromXhtml(coverFile, files)
                } else {
                    Image(absPath = coverFile.absPath, image = coverFile.data)
                }
            } else null
        }

        private fun parseCoverImageFromXhtml(
            coverFile: EpubFile,
            files: Map<String, EpubFile>
        ): Image? {
            val doc = Jsoup.parse(coverFile.data.inputStream(), "UTF-8", "")
            val imgTag = doc.selectFirst("img") ?: return null
            
            val imgSrc = imgTag.attr("src")
            val imgFile = files[imgSrc] ?: return null
            
            return Image(absPath = imgFile.absPath, image = imgFile.data)
        }

        private data class ManifestItem(
            val id: String,
            val absoluteFilePath: String,
            val mediaType: String,
            val properties: String
        )
    }
}
