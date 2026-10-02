package com.arfa_zuha.phonecleaner.ms321.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arfa_zuha.phonecleaner.ms321.data.FileManagerRepository
import com.arfa_zuha.phonecleaner.ms321.data.MediaFile
import com.arfa_zuha.phonecleaner.ms321.data.MediaFolder
import com.arfa_zuha.phonecleaner.ms321.data.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileManagerRepository(application)

    private val _currentMediaType = MutableStateFlow(MediaType.IMAGE)
    val currentMediaType: StateFlow<MediaType> = _currentMediaType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _allFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    val allFiles: StateFlow<List<MediaFile>> = _allFiles.asStateFlow()

    private val _folders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val folders: StateFlow<List<MediaFolder>> = _folders.asStateFlow()

    private val _isFolderView = MutableStateFlow(false)
    val isFolderView: StateFlow<Boolean> = _isFolderView.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _selectedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFileIds: StateFlow<Set<Long>> = _selectedFileIds.asStateFlow()

    private val _categorySizes = MutableStateFlow<Map<MediaType, Long>>(emptyMap())
    val categorySizes: StateFlow<Map<MediaType, Long>> = _categorySizes.asStateFlow()

    init {
        loadCategorySizes()
    }

    fun loadCategorySizes() {
        viewModelScope.launch {
            val sizes = repository.getCategorySizesMap()
            _categorySizes.value = sizes
        }
    }

    fun setMediaType(mediaType: MediaType) {
        _currentMediaType.value = mediaType
        _isFolderView.value = false
        _selectedFolder.value = null
        clearFileSelection()
        loadFiles(mediaType)
    }

    fun toggleViewMode() {
        _isFolderView.value = !_isFolderView.value
        _selectedFolder.value = null
        clearFileSelection()
    }

    fun selectFolder(folderName: String) {
        _selectedFolder.value = folderName
        _isFolderView.value = false
        clearFileSelection()
    }

    fun toggleFileSelection(fileId: Long) {
        val current = _selectedFileIds.value.toMutableSet()
        if (current.contains(fileId)) {
            current.remove(fileId)
        } else {
            current.add(fileId)
        }
        _selectedFileIds.value = current
    }

    fun selectAllFiles(files: List<MediaFile>) {
        if (_selectedFileIds.value.size == files.size) {
            clearFileSelection()
        } else {
            _selectedFileIds.value = files.map { it.id }.toSet()
        }
    }

    fun clearFileSelection() {
        _selectedFileIds.value = emptySet()
    }

    fun loadFiles(mediaType: MediaType) {
        viewModelScope.launch {
            _isLoading.value = true
            val files = repository.getMediaFiles(mediaType)
            _allFiles.value = files
            
            // Group by folder with calculated total bytes size
            val grouped = files.groupBy { it.folderName }
            val folderList = grouped.map { (folderName, folderFiles) ->
                val totalBytes = folderFiles.sumOf { it.size }
                MediaFolder(
                    folderName = folderName,
                    fileCount = folderFiles.size,
                    files = folderFiles,
                    thumbnailUri = folderFiles.firstOrNull()?.uri,
                    totalSizeBytes = totalBytes
                )
            }.sortedBy { it.folderName }
            _folders.value = folderList
            
            _isLoading.value = false
            loadCategorySizes()
        }
    }

    fun shareFile(uri: Uri, mimeType: String = "*/*") {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(shareIntent, "Share File")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(chooser)
    }

    fun shareMultipleFiles(selectedFiles: List<MediaFile>) {
        val uris = selectedFiles.map { it.uri }.toCollection(ArrayList())
        if (uris.isEmpty()) return

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Files")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(chooser)
    }

    fun deleteFile(file: MediaFile) {
        try {
            if (file.path.isNotEmpty()) {
                val f = File(file.path)
                if (f.exists()) f.delete()
            }
            getApplication<Application>().contentResolver.delete(file.uri, null, null)
            loadFiles(_currentMediaType.value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteSelectedFiles(selectedFiles: List<MediaFile>) {
        viewModelScope.launch {
            val contentResolver = getApplication<Application>().contentResolver
            for (file in selectedFiles) {
                try {
                    if (file.path.isNotEmpty()) {
                        val f = File(file.path)
                        if (f.exists()) f.delete()
                    }
                    contentResolver.delete(file.uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            clearFileSelection()
            loadFiles(_currentMediaType.value)
        }
    }
}
