package com.logan.vera.ui.reader

import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logan.vera.data.models.ChapterModel
import com.logan.vera.data.repository.BookRepository
import com.logan.vera.epub.parser.EpubParser
import com.logan.vera.epub.utils.EpubImageData
import com.logan.vera.ui.theme.Fonts
import com.logan.vera.ui.theme.ReaderTheme
import com.logan.vera.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "ReaderViewModel"

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: BookRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private var epubImages: Map<String, ByteArray> = emptyMap()
    private var currentBookId: String = ""
    private var lastSavedPosition: ReadingPosition? = null

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterModel>>(emptyList())
    val chapters = _chapters.asStateFlow()

    init {
        loadBook()
    }

    fun getImageData(imagePath: String): EpubImageData? {
        return epubImages[imagePath]?.let { 
            EpubImageData(currentBookId, imagePath, it)
        }
    }

    private fun loadBook() {
        viewModelScope.launch {
            try {
                val book = repository.allBooks.first().find { it.id == bookId }
                    ?: throw Exception("Book not found")

                currentBookId = book.id

                // Load the book content
                val epubBook = EpubParser.parse(File(book.filePath).inputStream())
                epubImages = epubBook.images.associate { it.absPath to it.image }

                // Update chapters
                _chapters.value = epubBook.chapters.mapIndexed { index, chapter ->
                    ChapterModel(
                        index = index,
                        title = chapter.title,
                        content = chapter.content
                    )
                }

                // Update UI state with saved position
                _uiState.update { state ->
                    state.copy(
                        title = book.title,
                        currentChapterIndex = book.lastReadChapterIndex,
                        scrollPosition = book.lastReadPosition,
                        fontSize = preferencesManager.fontSize,
                        fontFamily = preferencesManager.fontFamily,
                        theme = preferencesManager.theme,
                        isLoading = false,
                        coverImage = epubBook.coverImage?.let { 
                            EpubImageData(currentBookId, "cover", it.image)
                        }
                    )
                }

                lastSavedPosition = ReadingPosition(
                    chapterIndex = book.lastReadChapterIndex,
                    scrollOffset = book.lastReadPosition
                )

                Log.d(TAG, "Book loaded with position - Chapter: ${book.lastReadChapterIndex}, Position: ${book.lastReadPosition}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load book", e)
                _uiState.update { it.copy(
                    error = e.message ?: "Failed to load book",
                    isLoading = false
                )}
            }
        }
    }

    fun saveReadingProgress(position: ReadingPosition) {
        // Don't save if position hasn't changed
        if (position == lastSavedPosition) return
        
        viewModelScope.launch {
            try {
                repository.updateReadingProgress(
                    bookId = bookId,
                    chapterIndex = position.chapterIndex,
                    position = position.scrollOffset
                )
                
                lastSavedPosition = position
                _uiState.update { it.copy(
                    currentChapterIndex = position.chapterIndex,
                    scrollPosition = position.scrollOffset
                )}
                
                Log.d(TAG, "Reading progress saved - Chapter: ${position.chapterIndex}, Position: ${position.scrollOffset}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save reading progress", e)
            }
        }
    }

    fun updateFontSize(size: Float) {
        preferencesManager.fontSize = size
        _uiState.update { it.copy(fontSize = size) }
    }

    fun updateFont(font: FontFamily) {
        preferencesManager.fontFamily = font
        _uiState.update { it.copy(fontFamily = font) }
    }

    fun updateTheme(theme: ReaderTheme) {
        preferencesManager.theme = theme
        _uiState.update { it.copy(theme = theme) }
    }

    override fun onCleared() {
        super.onCleared()
        // Save final reading position when ViewModel is cleared
        _uiState.value.let { state ->
            viewModelScope.launch {
                try {
                    repository.updateReadingProgress(
                        bookId = bookId,
                        chapterIndex = state.currentChapterIndex,
                        position = state.scrollPosition
                    )
                    Log.d(TAG, "Final reading progress saved on ViewModel cleared")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save final reading progress", e)
                }
            }
        }
    }
}

data class ReaderUiState(
    val title: String = "",
    val currentChapterIndex: Int = 0,
    val scrollPosition: Float = 0f,
    val fontSize: Float = 16f,
    val fontFamily: FontFamily = Fonts.Roboto,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val isLoading: Boolean = true,
    val error: String? = null,
    val coverImage: EpubImageData? = null
)

data class ReadingPosition(
    val chapterIndex: Int,
    val scrollOffset: Float
)
