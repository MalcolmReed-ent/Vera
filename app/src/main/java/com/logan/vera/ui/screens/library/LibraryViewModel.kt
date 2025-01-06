package com.logan.vera.ui.screens.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logan.vera.data.models.BookModel
import com.logan.vera.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "LibraryViewModel"

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {
    val books = repository.allBooks
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
             initialValue = emptyList()
    )

    suspend fun addBook(fileBytes: ByteArray, fileName: String) {
        try {
            Log.d(TAG, "Adding book: $fileName")
            repository.addBook(fileBytes, fileName)
            Log.d(TAG, "Successfully added book: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding book: $fileName", e)
            throw e
        }
    }
}
