package com.logan.vera.epub.utils

data class EpubFile(
    val absPath: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EpubFile
        return absPath == other.absPath
    }

    override fun hashCode(): Int = absPath.hashCode()
}
