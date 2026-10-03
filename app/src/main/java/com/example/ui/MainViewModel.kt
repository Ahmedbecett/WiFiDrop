package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TransferDatabase
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.data.model.NearbyDevice
import com.example.data.model.TransferProgress
import com.example.data.model.TransferRecord
import com.example.data.repository.TransferRepository
import com.example.server.TransferClient
import com.example.server.WiFiDropServer
import com.example.util.AppLanguage
import com.example.util.FileUtils
import com.example.util.Language
import com.example.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
    HOME,
    SEND,
    RECEIVE,
    FILES,
    HISTORY,
    STORAGE,
    SETTINGS
}

data class SelectedFileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val formattedSize: String
)

enum class FileSortOrder {
    DATE_DESC,
    NAME_ASC,
    SIZE_DESC
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransferRepository
    val server: WiFiDropServer = WiFiDropServer(application, viewModelScope)
    private val transferClient = TransferClient(application)

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Language & Theme
    private val _currentLanguage = MutableStateFlow(AppLanguage.currentLanguage)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Send Files State
    private val _selectedFiles = MutableStateFlow<List<SelectedFileItem>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFileItem>> = _selectedFiles.asStateFlow()

    private val _customTargetIp = MutableStateFlow("")
    val customTargetIp: StateFlow<String> = _customTargetIp.asStateFlow()

    // Files Screen State
    private val _receivedFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val receivedFiles: StateFlow<List<FileInfo>> = _receivedFiles.asStateFlow()

    private val _fileCategoryFilter = MutableStateFlow(FileCategory.ALL)
    val fileCategoryFilter: StateFlow<FileCategory> = _fileCategoryFilter.asStateFlow()

    private val _fileSearchQuery = MutableStateFlow("")
    val fileSearchQuery: StateFlow<String> = _fileSearchQuery.asStateFlow()

    private val _fileSortOrder = MutableStateFlow(FileSortOrder.DATE_DESC)
    val fileSortOrder: StateFlow<FileSortOrder> = _fileSortOrder.asStateFlow()

    // History Screen State
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    val transferHistory: StateFlow<List<TransferRecord>>

    // Storage Screen State
    private val _storageStats = MutableStateFlow(FileUtils.getStorageInfo())
    val storageStats: StateFlow<Triple<Long, Long, Long>> = _storageStats.asStateFlow()

    private val _receivedFilesSize = MutableStateFlow(0L)
    val receivedFilesSize: StateFlow<Long> = _receivedFilesSize.asStateFlow()

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    // Device IP
    private val _localIp = MutableStateFlow(NetworkUtils.getLocalIpAddress(application) ?: "192.168.1.100")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    // Rewarded Session Perk
    private val _isPerkUnlocked = MutableStateFlow(false)
    val isPerkUnlocked: StateFlow<Boolean> = _isPerkUnlocked.asStateFlow()

    init {
        val db = TransferDatabase.getDatabase(application)
        repository = TransferRepository(db.transferDao())

        transferHistory = _historySearchQuery.flatMapLatest { query ->
            if (query.isBlank()) repository.allTransfers else repository.searchTransfers(query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Auto-start server for seamless experience
        server.startServer()
        refreshLocalIp()
        loadReceivedFiles()
        refreshStorageInfo()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.FILES) loadReceivedFiles()
        if (screen == Screen.STORAGE) refreshStorageInfo()
        if (screen == Screen.RECEIVE || screen == Screen.HOME) refreshLocalIp()
    }

    fun setLanguage(language: Language) {
        AppLanguage.currentLanguage = language
        _currentLanguage.value = language
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun setCustomTargetIp(ip: String) {
        _customTargetIp.value = ip
    }

    fun refreshLocalIp() {
        val ip = NetworkUtils.getLocalIpAddress(getApplication())
        if (ip != null) {
            _localIp.value = ip
        }
    }

    fun addSelectedFiles(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = uris.map { uri ->
                var name = "file_${System.currentTimeMillis()}"
                var size = 0L
                getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }
                SelectedFileItem(
                    uri = uri,
                    name = name,
                    size = size,
                    formattedSize = FileUtils.formatFileSize(size)
                )
            }
            _selectedFiles.value = _selectedFiles.value + list
        }
    }

    fun removeSelectedFile(item: SelectedFileItem) {
        _selectedFiles.value = _selectedFiles.value.filter { it != item }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    fun startSendingToDevice(targetIp: String, targetPort: Int, targetName: String) {
        val uris = _selectedFiles.value.map { it.uri }
        if (uris.isEmpty()) return

        viewModelScope.launch {
            transferClient.sendFiles(
                targetIp = targetIp,
                targetPort = targetPort,
                targetName = targetName,
                uris = uris,
                progressFlow = server.transferProgress as MutableStateFlow<TransferProgress>
            )
            clearSelectedFiles()
            loadReceivedFiles()
            refreshStorageInfo()
        }
    }

    fun cancelTransfer() {
        transferClient.cancel()
        server.cancelActiveTransfer()
    }

    fun pauseResumeTransfer(pause: Boolean) {
        transferClient.setPaused(pause)
    }

    // Files Screen
    fun loadReceivedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = FileUtils.listReceivedFiles(getApplication())
            _receivedFiles.value = files
        }
    }

    fun setFileCategory(category: FileCategory) {
        _fileCategoryFilter.value = category
    }

    fun setFileSearchQuery(query: String) {
        _fileSearchQuery.value = query
    }

    fun setFileSortOrder(order: FileSortOrder) {
        _fileSortOrder.value = order
    }

    fun deleteFile(file: FileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.deleteFile(getApplication(), file)
            loadReceivedFiles()
            refreshStorageInfo()
        }
    }

    fun renameFile(file: FileInfo, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.renameFile(getApplication(), file, newName)
            loadReceivedFiles()
        }
    }

    // History
    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteTransfer(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Storage
    fun refreshStorageInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _storageStats.value = FileUtils.getStorageInfo()
            _receivedFilesSize.value = FileUtils.getReceivedFilesSize(getApplication())
            _cacheSize.value = FileUtils.getCacheSize(getApplication())
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.clearCache(getApplication())
            refreshStorageInfo()
        }
    }

    fun unlockPerk() {
        _isPerkUnlocked.value = true
    }

    override fun onCleared() {
        super.onCleared()
        server.stopServer()
    }
}
