package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.ui.FileSortOrder
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.FileItemRow
import com.example.ui.theme.BrandCyan
import com.example.util.AppLanguage
import com.example.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(Screen.HOME)
    }

    val context = LocalContext.current
    val receivedFiles by viewModel.receivedFiles.collectAsState()
    val selectedCategory by viewModel.fileCategoryFilter.collectAsState()
    val searchQuery by viewModel.fileSearchQuery.collectAsState()
    val sortOrder by viewModel.fileSortOrder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileInfo?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<FileInfo?>(null) }

    // Filter and sort files
    val filteredFiles = remember(receivedFiles, selectedCategory, searchQuery, sortOrder) {
        var list = receivedFiles
        if (selectedCategory != FileCategory.ALL) {
            list = list.filter { it.category == selectedCategory }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        when (sortOrder) {
            FileSortOrder.DATE_DESC -> list.sortedByDescending { it.lastModified }
            FileSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            FileSortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(AppLanguage.getString("nav_files"), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadReceivedFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Date (Newest)") },
                                onClick = {
                                    viewModel.setFileSortOrder(FileSortOrder.DATE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Name (A-Z)") },
                                onClick = {
                                    viewModel.setFileSortOrder(FileSortOrder.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size (Largest)") },
                                onClick = {
                                    viewModel.setFileSortOrder(FileSortOrder.SIZE_DESC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setFileSearchQuery(it) },
                placeholder = { Text("Search files by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setFileSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("file_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FileCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setFileCategory(category) },
                        label = {
                            Text(
                                text = when (category) {
                                    FileCategory.ALL -> "All (${receivedFiles.size})"
                                    FileCategory.IMAGE -> "🖼️ Images"
                                    FileCategory.VIDEO -> "🎬 Videos"
                                    FileCategory.DOCUMENT -> "📄 Documents"
                                    FileCategory.AUDIO -> "🎵 Audio"
                                    FileCategory.ARCHIVE -> "📦 Archives"
                                    FileCategory.OTHER -> "📁 Other"
                                },
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandCyan.copy(alpha = 0.2f),
                            selectedLabelColor = BrandCyan
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Files List or Empty State
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No files match \"$searchQuery\"" else "No received files yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Files sent to this phone from PC or another device will appear here organized by category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        FileItemRow(
                            fileInfo = file,
                            onOpen = { FileUtils.openFile(context, file) },
                            onShare = { FileUtils.shareFile(context, file) },
                            onRename = {
                                fileToRename = file
                                newFileName = file.name
                            },
                            onDelete = { fileToDelete = file }
                        )
                    }
                }
            }
        }
    }

    // Rename File Dialog
    fileToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("New file name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.renameFile(target, newFileName.trim())
                            fileToRename = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete \"${target.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(target)
                        fileToDelete = null
                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
