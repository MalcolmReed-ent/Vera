package com.logan.vera.epub.utils

import android.graphics.drawable.BitmapDrawable
import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

class EpubImageFetcher(
    private val options: Options,
    private val data: EpubImageData
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bitmap = BitmapFactory.decodeByteArray(data.imageBytes, 0, data.imageBytes.size)
        val drawable = BitmapDrawable(options.context.resources, bitmap)
        
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<EpubImageData> {
        override fun create(data: EpubImageData, options: Options, imageLoader: ImageLoader): Fetcher {
            return EpubImageFetcher(options, data)
        }
    }
}

data class EpubImageData(
    val bookId: String,
    val imagePath: String,
    val imageBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EpubImageData
        return bookId == other.bookId && imagePath == other.imagePath
    }

    override fun hashCode(): Int {
        var result = bookId.hashCode()
        result = 31 * result + imagePath.hashCode()
        return result
    }
}
