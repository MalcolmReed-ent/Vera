package com.logan.vera.epub.utils

class BookTextMapper {
    data class ImgEntry(
        val path: String,
        val yrel: Float
    ) {
        fun toXMLString(): String = "<img path=\"$path\" yrel=\"$yrel\" />"
    }

    companion object {
        fun parseImgTag(xml: String): ImgEntry? {
            val pathRegex = "path=\"([^\"]*)\"".toRegex()
            val yrelRegex = "yrel=\"([^\"]*)\"".toRegex()

            val path = pathRegex.find(xml)?.groupValues?.get(1) ?: return null
            val yrel = yrelRegex.find(xml)?.groupValues?.get(1)?.toFloatOrNull() ?: return null

            return ImgEntry(path, yrel)
        }
    }
}
