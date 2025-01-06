package com.logan.vera.epub.models

data class EpubBook(
    val fileName: String,
    val title: String,
    val author: String?,
    val description: String?,
    val coverImage: Image?,
    val chapters: List<Chapter>,
    val images: List<Image>
) {
    data class Image(
        val absPath: String,
        val image: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Image
            return absPath == other.absPath
        }

        override fun hashCode(): Int = absPath.hashCode()
    }

    data class Chapter(
        val link: String,
        val title: String,
        val content: String
    )

    data class ToCEntry(
        val chapterTitle: String,
        val chapterLink: String
    )

    data class ManifestItem(
        val id: String,
        val absPath: String,
        val mediaType: String,
        val properties: String
    )
}
