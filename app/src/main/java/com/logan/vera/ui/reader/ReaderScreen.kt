package com.logan.vera.ui.reader

import android.app.Activity
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.logan.vera.data.models.ChapterModel
import com.logan.vera.epub.utils.BookTextMapper
import com.logan.vera.ui.theme.ReaderTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    bookId: String,
    onNavigateUp: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var isInitialized by remember { mutableStateOf(false) }
    var showBars by remember { mutableStateOf(true) }
    val coverHeight = 300.dp

    // System UI controller
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        
        controller.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Initial scroll to saved position
    LaunchedEffect(chapters, uiState.currentChapterIndex, uiState.scrollPosition) {
        if (chapters.isNotEmpty() && !isInitialized) {
            listState.scrollToItem(
                index = uiState.currentChapterIndex,
                scrollOffset = with(density) { uiState.scrollPosition.dp.toPx().toInt() }
            )
            isInitialized = true
        }
    }

    // Save reading progress when scrolling (debounced)
    LaunchedEffect(listState) {
        snapshotFlow { 
            ReadingPosition(
                chapterIndex = listState.firstVisibleItemIndex,
                scrollOffset = with(density) { listState.firstVisibleItemScrollOffset.toDp().value }
            )
        }
        .distinctUntilChanged()
        .collect { position ->
            if (isInitialized && position.chapterIndex >= 0) {
                viewModel.saveReadingProgress(position)
            }
        }
    }

    // Back handler
    BackHandler {
        coroutineScope.launch {
            val position = ReadingPosition(
                chapterIndex = listState.firstVisibleItemIndex,
                scrollOffset = with(density) { listState.firstVisibleItemScrollOffset.toDp().value }
            )
            viewModel.saveReadingProgress(position)
            onNavigateUp()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = showBars,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ReaderTopBar(
                        title = uiState.title,
                        onNavigateUp = {
                            coroutineScope.launch {
                                val position = ReadingPosition(
                                    chapterIndex = listState.firstVisibleItemIndex,
                                    scrollOffset = with(density) { 
                                        listState.firstVisibleItemScrollOffset.toDp().value 
                                    }
                                )
                                viewModel.saveReadingProgress(position)
                                onNavigateUp()
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showBars = !showBars
                        val window = (view.context as Activity).window
                        val controller = WindowCompat.getInsetsController(window, view)
                        if (showBars) {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                        } else {
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                        }
                    }
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    } ?: LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(uiState.theme.background),
                        userScrollEnabled = true
                    ) {
                        // Cover image
                        item(key = "cover") {
                            uiState.coverImage?.let { coverImageData ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                ) {
                                    AsyncImage(
                                        model = coverImageData,
                                        contentDescription = "Book cover",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(coverHeight),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                HorizontalDivider()
                            }
                        }

                        // Chapters
                        items(
                            items = chapters,
                            key = { it.index }
                        ) { chapter ->
                            ChapterContent(
                                chapter = chapter,
                                fontSize = uiState.fontSize,
                                fontFamily = uiState.fontFamily,
                                theme = uiState.theme,
                                viewModel = viewModel,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterContent(
    chapter: ChapterModel,
    fontSize: Float,
    fontFamily: FontFamily,
    theme: ReaderTheme,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.background)
    ) {
        if (chapter.title.isNotEmpty()) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = fontFamily,
                color = theme.text,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        val parts = remember(chapter.content) {
            chapter.content.split("<img")
        }
        
        parts.forEachIndexed { index, part ->
            if (index == 0) {
                Text(
                    text = part,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5).sp,
                    fontFamily = fontFamily,
                    color = theme.text
                )
            } else {
                val imgEndIndex = part.indexOf("/>")
                if (imgEndIndex != -1) {
                    val imgTag = part.substring(0, imgEndIndex + 2)
                    val text = part.substring(imgEndIndex + 2)
                    
                    BookTextMapper.parseImgTag(imgTag)?.let { img ->
                        val imageData = viewModel.getImageData(img.path)
                        if (imageData != null) {
                            AsyncImage(
                                model = imageData,
                                contentDescription = "Book image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f / img.yrel)
                                    .padding(vertical = 8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    
                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.5).sp,
                            fontFamily = fontFamily,
                            color = theme.text
                        )
                    }
                }
            }
        }
    }
}
