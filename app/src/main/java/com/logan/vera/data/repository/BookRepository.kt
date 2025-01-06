package com.logan.vera.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.logan.vera.data.database.dao.BookDao
import com.logan.vera.data.database.entities.Book
import com.logan.vera.data.models.BookModel
import com.logan.vera.data.models.SearchResult
import com.logan.vera.data.models.TextMatch
import com.logan.vera.epub.parser.EpubParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BookRepository"

@Singleton
class BookRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao
) {
    private val booksDir = File(context.filesDir, "books").apply {
        if (!exists()) {
            if (!mkdirs()) {
                Log.e(TAG, "Failed to create books directory")
            }
        }
    }

    val allBooks: Flow<List<BookModel>> = bookDao.getAllBooks()
        .map { books -> books.map { it.toModel() } }

    suspend fun addBookFromUri(uri: Uri, fileName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting to add book from URI: $fileName")
                
                val fileBytes = context.contentResolver.openInputStream(uri)?.use { 
                    it.readBytes() 
                } ?: throw Exception("Failed to read file")
                
                addBook(fileBytes, fileName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add book from URI", e)
                throw e
            }
        }
    }

    suspend fun addBook(fileBytes: ByteArray, fileName: String): String {
        return withContext(Dispatchers.IO) {
            val bookId = UUID.randomUUID().toString()
            val bookFile = File(booksDir, "$bookId.epub")

            try {
                Log.d(TAG, "Starting to add book: $fileName")

                bookFile.parentFile?.mkdirs()

                try {
                    bookFile.writeBytes(fileBytes)
                    Log.d(TAG, "Successfully wrote file to: ${bookFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write file", e)
                    throw Exception("Failed to save book file: ${e.message}")
                }

                val epubBook = try {
                    bookFile.inputStream().use { EpubParser.parse(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse EPUB", e)
                    bookFile.delete()
                    throw Exception("Invalid EPUB file format: ${e.message}")
                }

                Log.d(TAG, "Successfully parsed EPUB: ${epubBook.title}")

                val coverPath = epubBook.coverImage?.let { cover ->
                    try {
                        saveCoverImage(cover.image, bookId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to save cover image", e)
                        null
                    }
                }

                val book = Book(
                    id = bookId,
                    title = epubBook.title.ifEmpty { fileName },
                    author = epubBook.author,
                    description = epubBook.description,
                    coverPath = coverPath,
                    filePath = bookFile.absolutePath,
                    totalChapters = epubBook.chapters.size
                )

                try {
                    bookDao.insertBook(book)
                    Log.d(TAG, "Successfully saved book to database")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save to database", e)
                    bookFile.delete()
                    coverPath?.let { File(it).delete() }
                    throw Exception("Failed to save book to database: ${e.message}")
                }

                bookId
            } catch (e: Exception) {
                Log.e(TAG, "Error adding book", e)
                bookFile.delete()
                throw e
            }
        }
    }

    private fun saveCoverImage(imageData: ByteArray, bookId: String): String {
        val coverFile = File(booksDir, "$bookId.cover")
        coverFile.writeBytes(imageData)
        return coverFile.absolutePath
    }

    suspend fun deleteBook(bookId: String) {
        withContext(Dispatchers.IO) {
            try {
                val book = bookDao.getBook(bookId) ?: return@withContext

                val bookFile = File(book.filePath)
                if (bookFile.exists() && !bookFile.delete()) {
                    Log.w(TAG, "Failed to delete book file: ${book.filePath}")
                }

                book.coverPath?.let { coverPath ->
                    val coverFile = File(coverPath)
                    if (coverFile.exists() && !coverFile.delete()) {
                        Log.w(TAG, "Failed to delete cover file: $coverPath")
                    }
                }

                bookDao.deleteBook(book)
                Log.d(TAG, "Successfully deleted book: $bookId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete book: $bookId", e)
                throw e
            }
        }
    }

    suspend fun updateReadingProgress(
        bookId: String,
        chapterIndex: Int,
        position: Float,
        timeSpent: Long = 0
    ) {
        try {
            val book = bookDao.getBook(bookId) ?: return
            val progress = if (book.totalChapters > 0) {
                (chapterIndex.toFloat() / book.totalChapters.toFloat()) + 
                (position / book.totalChapters.toFloat())
            } else 0f

            bookDao.updateReadingProgress(
                bookId = bookId,
                chapterIndex = chapterIndex,
                position = position,
                totalChapters = book.totalChapters,
                progress = progress,
                timeSpent = timeSpent
            )
            Log.d(TAG, "Updated reading progress for book: $bookId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update reading progress", e)
            throw e
        }
    }

    fun searchBooks(query: String): Flow<List<BookModel>> =
        bookDao.searchBooks(query)
            .map { books -> books.map { it.toModel() } }

    fun searchInBookContent(bookId: String, query: String): Flow<List<SearchResult>> = flow {
        val book = bookDao.getBook(bookId) ?: return@flow
        val searchResults = withContext(Dispatchers.IO) {
            val epubBook = File(book.filePath).inputStream().use { EpubParser.parse(it) }
            epubBook.chapters.mapIndexedNotNull { index, chapter ->
                val matches = findMatches(chapter.content, query)
                if (matches.isEmpty()) null
                else SearchResult(
                    chapterIndex = index,
                    chapterTitle = chapter.title,
                    matches = matches
                )
            }
        }
        emit(searchResults)
    }

    private fun findMatches(content: String, query: String): List<TextMatch> {
        val matches = mutableListOf<TextMatch>()
        var index = content.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            val contextStart = (index - 50).coerceAtLeast(0)
            val contextEnd = (index + query.length + 50).coerceAtMost(content.length)
            val matchText = content.substring(contextStart, contextEnd)
            
            matches.add(TextMatch(
                text = matchText,
                position = index
            ))
            
            index = content.indexOf(query, index + 1, ignoreCase = true)
        }
        return matches
    }

    private fun Book.toModel() = BookModel(
        id = id,
        title = title,
        author = author,
        description = description,
        coverPath = coverPath,
        filePath = filePath,
        lastReadChapterIndex = lastReadChapterIndex,
        lastReadPosition = lastReadPosition,
        dateAdded = dateAdded,
        lastAccessed = lastAccessed,
        totalChapters = totalChapters,
        readProgress = readProgress,
        timeSpentReading = timeSpentReading,
        lastReadDate = lastReadDate
    )
}
