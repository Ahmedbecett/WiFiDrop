package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.BrandCyan
import com.example.util.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PdfReadingMode {
    NORMAL,
    SEPIA,
    NIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfFile: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var pageCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val pageBitmaps = remember { mutableStateListOf<Bitmap?>() }
    var readingMode by remember { mutableStateOf(PdfReadingMode.NORMAL) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var targetPageInput by remember { mutableStateOf("") }

    val currentPage by remember {
        derivedStateOf {
            if (pageCount == 0) 1 else (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount)
        }
    }

    // Load PDF metadata and render pages
    LaunchedEffect(pdfFile) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val count = PdfUtils.getPageCount(pdfFile)
            pageCount = count
            pageBitmaps.clear()
            for (i in 0 until count) {
                pageBitmaps.add(null)
            }
        }
        isLoading = false

        // Progressively load page bitmaps
        withContext(Dispatchers.IO) {
            for (i in 0 until pageCount) {
                val bmp = PdfUtils.renderPageToBitmap(pdfFile, i)
                withContext(Dispatchers.Main) {
                    if (i < pageBitmaps.size) {
                        pageBitmaps[i] = bmp
                    }
                }
            }
        }
    }

    // Background color based on reading mode
    val backgroundColor = when (readingMode) {
        PdfReadingMode.NORMAL -> MaterialTheme.colorScheme.background
        PdfReadingMode.SEPIA -> Color(0xFFFBF0D9)
        PdfReadingMode.NIGHT -> Color(0xFF121212)
    }

    val pageCardColor = when (readingMode) {
        PdfReadingMode.NORMAL -> Color.White
        PdfReadingMode.SEPIA -> Color(0xFFF4ECD8)
        PdfReadingMode.NIGHT -> Color(0xFF1E1E1E)
    }

    val colorFilter = when (readingMode) {
        PdfReadingMode.NORMAL -> null
        PdfReadingMode.SEPIA -> {
            // Warm sepia tone matrix
            val matrix = ColorMatrix(
                floatArrayOf(
                    0.90f, 0.05f, 0.05f, 0f, 25f,
                    0.05f, 0.85f, 0.05f, 0f, 20f,
                    0.05f, 0.05f, 0.70f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(matrix)
        }
        PdfReadingMode.NIGHT -> {
            // High contrast inverted dark mode
            val matrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(matrix)
        }
    }

    Scaffold(
        modifier = modifier.testTag("pdf_viewer_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pdfFile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pageCount > 0) "Page $currentPage of $pageCount" else "Loading document...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Reading Mode Toggle
                    IconButton(
                        onClick = {
                            readingMode = when (readingMode) {
                                PdfReadingMode.NORMAL -> PdfReadingMode.SEPIA
                                PdfReadingMode.SEPIA -> PdfReadingMode.NIGHT
                                PdfReadingMode.NIGHT -> PdfReadingMode.NORMAL
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (readingMode) {
                                PdfReadingMode.NORMAL -> Icons.Default.LightMode
                                PdfReadingMode.SEPIA -> Icons.Default.Brightness4
                                PdfReadingMode.NIGHT -> Icons.Default.DarkMode
                            },
                            contentDescription = "Reading Mode"
                        )
                    }

                    // Share PDF
                    IconButton(
                        onClick = {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }

                    // Print PDF
                    IconButton(
                        onClick = {
                            try {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                printManager?.print(
                                    pdfFile.nameWithoutExtension,
                                    object : PrintDocumentAdapter() {
                                        override fun onLayout(
                                            oldAttributes: PrintAttributes?,
                                            newAttributes: PrintAttributes?,
                                            cancellationSignal: android.os.CancellationSignal?,
                                            callback: LayoutResultCallback?,
                                            extras: android.os.Bundle?
                                        ) {
                                            callback?.onLayoutFinished(
                                                android.print.PrintDocumentInfo.Builder(pdfFile.name)
                                                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                                    .setPageCount(pageCount)
                                                    .build(),
                                                true
                                            )
                                        }

                                        override fun onWrite(
                                            pages: Array<out android.print.PageRange>?,
                                            destination: android.os.ParcelFileDescriptor?,
                                            cancellationSignal: android.os.CancellationSignal?,
                                            callback: WriteResultCallback?
                                        ) {
                                            try {
                                                pdfFile.inputStream().use { input ->
                                                    destination?.let { dest ->
                                                        java.io.FileOutputStream(dest.fileDescriptor).use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                }
                                                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                            } catch (e: Exception) {
                                                callback?.onWriteFailed(e.message)
                                            }
                                        }
                                    },
                                    null
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Print not supported on this device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print Document")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            if (isLoading && pageCount == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BrandCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading PDF Document...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (pageCount == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unable to read this PDF document.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // PDF Pages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(0.8f, 3.0f)
                            }
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(pageBitmaps) { index, bitmap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = pageCardColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth,
                                        colorFilter = colorFilter
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = BrandCyan,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }

                                // Page Number Tag
                                Text(
                                    text = "- ${index + 1} -",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Floating Bottom Control Bar (Page Indicator & Zoom)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom Out
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.8f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom Out",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Page Jump Button
                        Text(
                            text = "$currentPage / $pageCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    targetPageInput = currentPage.toString()
                                    showJumpDialog = true
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )

                        // Zoom In
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom In",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                Column {
                    Text(
                        text = "Enter a page number between 1 and $pageCount:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetPageInput,
                        onValueChange = { targetPageInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val page = targetPageInput.toIntOrNull()
                        if (page != null && page in 1..pageCount) {
                            scope.launch {
                                listState.animateScrollToItem(page - 1)
                            }
                            showJumpDialog = false
                        } else {
                            Toast.makeText(context, "Invalid page number", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
