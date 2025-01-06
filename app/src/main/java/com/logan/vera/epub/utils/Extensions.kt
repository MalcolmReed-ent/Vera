package com.logan.vera.epub.utils

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.zip.ZipInputStream

val String.decodedURL: String
    get() = URLDecoder.decode(this, "UTF-8")

val String.encodedURL: String
    get() = URLEncoder.encode(this, "UTF-8")

fun String.asFileName(): String = replace(Regex("[^a-zA-Z0-9.-]"), "_")

fun ZipInputStream.entries() = generateSequence { nextEntry }
