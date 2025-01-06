package com.logan.vera.epub.parser

import android.util.Log
import com.logan.vera.epub.models.EpubBook
import com.logan.vera.epub.models.EpubBook.*
import com.logan.vera.epub.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.InputStream
import kotlin.io.path.invariantSeparatorsPathString

private const val TAG = "EpubParser"

class EpubParser {
    companion object {
        internal suspend fun getZipFiles(
            inputStream: InputStream
        ): Map<String, EpubFile> = withContext(Dispatchers.IO) {
            inputStream.use { stream ->
                stream.buffered().let {
                    java.util.zip.ZipInputStream(it).use { zipInputStream ->
                        zipInputStream
                            .entries()
                            .filterNot { it.isDirectory }
                            .map { EpubFile(absPath = it.name, data = zipInputStream.readBytes()) }
                            .associateBy { it.absPath }
                    }
                }
            }
        }

        @Throws(Exception::class)
        suspend fun parse(inputStream: InputStream): EpubBook = withContext(Dispatchers.Default) {
            try {
                val files = getZipFiles(inputStream)

                // Parse container.xml to get OPF file path
                val container = files["META-INF/container.xml"]
                    ?: throw Exception("META-INF/container.xml file missing")

                val opfFilePath = parseXMLFile(container.data)
                    ?.selectFirstTag("rootfile")
                    ?.getAttributeValue("full-path")
                    ?.decodedURL ?: throw Exception("Invalid container.xml file")

                // Extract rootPath and get OPF file
                val rootPath = opfFilePath.substringBefore('/', "")
                val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

                // Parse OPF file
                val document = parseXMLFile(opfFile.data)
                    ?: throw Exception(".opf file failed to parse data")
                
                // Extract metadata
                val metadata = document.selectFirstTag("metadata")
                    ?: throw Exception(".opf file metadata section missing")
                val manifest = document.selectFirstTag("manifest")
                    ?: throw Exception(".opf file manifest section missing")
                val spine = document.selectFirstTag("spine")
                    ?: throw Exception(".opf file spine section missing")
                val guide = document.selectFirstTag("guide")

                // Parse basic metadata
                val metadataTitle = metadata.selectFirstChildTag("dc:title")?.textContent
                    ?: metadata.selectFirstChildTag("title")?.textContent
                    ?: "Unknown Title"
                val metadataCreator = metadata.selectFirstChildTag("dc:creator")?.textContent
                    ?: metadata.selectFirstChildTag("creator")?.textContent
                val metadataDesc = metadata.selectFirstChildTag("dc:description")?.textContent
                    ?: metadata.selectFirstChildTag("description")?.textContent
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
                val manifestItems = manifest.selectChildTag("item").map {
                    ManifestItem(
                        id = it.getAttribute("id"),
                        absPath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
                        mediaType = it.getAttribute("media-type"),
                        properties = it.getAttribute("properties")
                    )
                }.associateBy { it.id }

                // Parse cover image
                val coverImage = parseCoverImage(
                    files,
                    manifestItems,
                    metadataCoverId,
                    guide,
                    rootPath
                )

                // Parse NCX file for table of contents
                val ncxFilePath = manifestItems["ncx"]?.absPath 
                    ?: manifestItems["toc"]?.absPath
                    ?: manifestItems.values.firstOrNull { 
                        it.mediaType.contains("ncx") || it.absPath.endsWith(".ncx") 
                    }?.absPath
                
                val tocEntries = if (ncxFilePath != null && files.containsKey(ncxFilePath)) {
                    try {
                        parseTableOfContents(files[ncxFilePath]!!, rootPath)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse TOC, using fallback", e)
                        createFallbackToc(spine)
                    }
                } else {
                    Log.d(TAG, "No NCX file found, using fallback TOC")
                    createFallbackToc(spine)
                }

                // Parse chapters
                val chapters = parseChapters(
                    spine,
                    manifestItems,
                    files,
                    tocEntries,
                    rootPath
                )

                // Parse images
                val images = parseImages(files, manifestItems)

                return@withContext EpubBook(
                    fileName = metadataTitle.asFileName(),
                    title = metadataTitle,
                    author = metadataCreator,
                    description = metadataDesc,
                    coverImage = coverImage,
                    chapters = chapters,
                    images = images
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing EPUB", e)
                throw e
            }
        }

        private fun createFallbackToc(spine: org.w3c.dom.Element): List<ToCEntry> {
            return spine.selectChildTag("itemref")
                .mapIndexed { index, _ -> 
                    ToCEntry("Chapter ${index + 1}", "") 
                }
        }

        private fun parseTableOfContents(ncxFile: EpubFile, rootPath: String): List<ToCEntry> {
            try {
                // First try parsing as XML
                val element = parseXMLFile(ncxFile.data)
                if (element != null) {
                    val navMap = element.selectFirstTag("navMap")
                        ?: throw Exception("Invalid NCX file: navMap not found")
                    
                    return navMap.selectChildTag("navPoint").map { navPoint ->
                        val title = navPoint.selectFirstChildTag("navLabel")
                            ?.selectFirstChildTag("text")?.textContent ?: ""
                        var link = navPoint.selectFirstChildTag("content")
                            ?.getAttributeValue("src")?.decodedURL ?: ""
                        
                        if (!link.startsWith(rootPath)) {
                            link = "$rootPath/$link"
                        }
                        ToCEntry(title, link)
                    }
                }

                // Fallback to Jsoup parsing if XML parsing fails
                val doc = Jsoup.parse(ncxFile.data.inputStream(), "UTF-8", "")
                val navMap = doc.selectFirst("navMap") 
                    ?: throw Exception("Invalid NCX file: navMap not found")

                return navMap.select("navPoint").map { navPoint ->
                    val title = navPoint.selectFirst("navLabel")?.selectFirst("text")?.text() ?: ""
                    var link = navPoint.selectFirst("content")?.attr("src") ?: ""
                    if (!link.startsWith(rootPath)) {
                        link = "$rootPath/$link"
                    }
                    ToCEntry(title, link)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing table of contents", e)
                throw e
            }
        }

        private fun parseCoverImage(
            files: Map<String, EpubFile>,
            manifestItems: Map<String, ManifestItem>,
            metadataCoverId: String?,
            guide: org.w3c.dom.Element?,
            rootPath: String
        ): Image? {
            try {
                // Try to get cover from metadata
                var coverImage = manifestItems[metadataCoverId]
                    ?.let { files[it.absPath] }
                    ?.let { Image(absPath = it.absPath, image = it.data) }

                // If no cover found in metadata, try guide
                if (coverImage == null) {
                    val coverHref = guide?.selectChildTag("reference")
                        ?.find { it.getAttribute("type") == "cover" }
                        ?.getAttributeValue("href")
                        ?.decodedURL

                    val coverPath = if (coverHref != null) {
                        if (!coverHref.startsWith(rootPath)) {
                            "$rootPath/$coverHref"
                        } else coverHref
                    } else null

                    coverImage = coverPath?.let { files[it] }?.let { 
                        if (it.absPath.endsWith(".html", true) || it.absPath.endsWith(".xhtml", true)) {
                            val doc = Jsoup.parse(it.data.inputStream(), "UTF-8", "")
                            val imgSrc = doc.selectFirst("img")?.attr("src") ?: return@let null
                            val imgPath = if (!imgSrc.startsWith(rootPath)) "$rootPath/$imgSrc" else imgSrc
                            files[imgPath]?.let { imgFile -> Image(absPath = imgFile.absPath, image = imgFile.data) }
                        } else {
                            Image(absPath = it.absPath, image = it.data)
                        }
                    }

                    // If still no cover, try finding first image in manifest
                    if (coverImage == null) {
                        coverImage = manifestItems.values
                            .firstOrNull { it.mediaType.startsWith("image/") }
                            ?.let { files[it.absPath] }
                            ?.let { Image(absPath = it.absPath, image = it.data) }
                    }
                }

                return coverImage
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing cover image", e)
                return null
            }
        }

        private suspend fun parseChapters(
            spine: org.w3c.dom.Element,
            manifestItems: Map<String, ManifestItem>,
            files: Map<String, EpubFile>,
            tocEntries: List<ToCEntry>,
            rootPath: String
        ): List<Chapter> = withContext(Dispatchers.Default) {
            val chapters = mutableListOf<Chapter>()

            spine.selectChildTag("itemref").forEach { itemRef ->
                try {
                    val itemId = itemRef.getAttribute("idref")
                    val spineItem = manifestItems[itemId]

                    if (spineItem != null && isChapter(spineItem)) {
                        var spineUrl = spineItem.absPath
                        if (!spineUrl.startsWith(rootPath)) {
                            spineUrl = "$rootPath/$spineUrl"
                        }

                        val tocEntry = findTocEntryForChapter(tocEntries, spineUrl)
                        val parser = EpubXMLParser(spineUrl, files[spineUrl]?.data ?: ByteArray(0), files)
                        val res = parser.parseAsDocument()

                        chapters.add(Chapter(
                            link = tocEntry?.chapterLink ?: spineUrl,
                            title = tocEntry?.chapterTitle ?: "Chapter ${chapters.size + 1}",
                            content = res.body
                        ))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing chapter", e)
                    // Continue with next chapter
                }
            }

            chapters
        }

        private fun isChapter(item: ManifestItem): Boolean {
            val extension = item.absPath.substringAfterLast('.').lowercase()
            return extension in listOf("xhtml", "xml", "html", "htm")
        }

        private fun findTocEntryForChapter(
            tocEntries: List<ToCEntry>,
            chapterUrl: String
        ): ToCEntry? {
            val chapterUrlWithoutFragment = chapterUrl.substringBefore('#')
            return tocEntries.firstOrNull {
                it.chapterLink.substringBefore('#')
                    .equals(chapterUrlWithoutFragment, ignoreCase = true)
            }
        }

        private fun parseImages(
            files: Map<String, EpubFile>,
            manifestItems: Map<String, ManifestItem>
        ): List<Image> {
            val imageExtensions = listOf("png", "gif", "raw", "jpg", "jpeg", "webp", "svg")
                .map { ".$it" }

            val unlistedImages = files
                .asSequence()
                .filter { (_, file) ->
                    imageExtensions.any { file.absPath.endsWith(it, ignoreCase = true) }
                }
                .map { (_, file) ->
                    Image(absPath = file.absPath, image = file.data)
                }

            val listedImages = manifestItems.asSequence()
                .map { it.value }
                .filter { it.mediaType.startsWith("image") }
                .mapNotNull { files[it.absPath] }
                .map { Image(absPath = it.absPath, image = it.data) }

            return (unlistedImages + listedImages).distinct().toList()
        }

        private data class ManifestItem(
            val id: String,
            val absPath: String,
            val mediaType: String,
            val properties: String
        )
    }
}
