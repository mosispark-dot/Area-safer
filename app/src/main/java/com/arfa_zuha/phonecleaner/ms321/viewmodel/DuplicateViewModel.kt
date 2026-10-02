package com.arfa_zuha.phonecleaner.ms321.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arfa_zuha.phonecleaner.ms321.data.DuplicateFile
import com.arfa_zuha.phonecleaner.ms321.data.DuplicateGroup
import com.arfa_zuha.phonecleaner.ms321.data.FileCategory
import com.arfa_zuha.phonecleaner.ms321.utils.DuplicateFinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

class DuplicateViewModel(application: Application) : AndroidViewModel(application) {

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _scanProgressPercent = MutableStateFlow(0)
    val scanProgressPercent: StateFlow<Int> = _scanProgressPercent

    private val _scanPhaseMessage = MutableStateFlow("Initializing...")
    val scanPhaseMessage: StateFlow<String> = _scanPhaseMessage

    private val _currentCategory = MutableStateFlow<FileCategory?>(null)
    val currentCategory: StateFlow<FileCategory?> = _currentCategory

    private val _selectedGroup = MutableStateFlow<DuplicateGroup?>(null)
    val selectedGroup: StateFlow<DuplicateGroup?> = _selectedGroup

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter

    fun selectGroup(group: DuplicateGroup) {
        _selectedGroup.value = group
    }

    fun clearSelectedGroup() {
        _selectedGroup.value = null
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    fun scanDuplicates(category: FileCategory) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null
            _currentCategory.value = category
            _scanProgressPercent.value = 5
            _scanPhaseMessage.value = "Starting ${category.label} Scanner..."

            try {
                val groups = withTimeoutOrNull(180000L) {
                    DuplicateFinder.findDuplicates(getApplication(), category) { percent, msg ->
                        _scanProgressPercent.value = percent
                        _scanPhaseMessage.value = msg
                    }
                }
                _duplicateGroups.value = groups ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _duplicateGroups.value = emptyList()
            } finally {
                _scanProgressPercent.value = 100
                _scanPhaseMessage.value = "Scan Complete"
                _isLoading.value = false
            }
        }
    }

    fun resetCategory() {
        _currentCategory.value = null
        _duplicateGroups.value = emptyList()
        _statusMessage.value = null
        _selectedFilter.value = "ALL"
        _scanProgressPercent.value = 0
    }

    fun toggleSelection(file: DuplicateFile) {
        val updatedGroups = _duplicateGroups.value.map { group ->
            val updatedFiles = group.files.map { f ->
                if (f.id == file.id) f.copy(isSelected = !f.isSelected) else f
            }
            group.copy(files = updatedFiles)
        }
        _duplicateGroups.value = updatedGroups

        if (_selectedGroup.value != null) {
            val updatedSelectedGroup = updatedGroups.find { it.hash == _selectedGroup.value!!.hash }
            _selectedGroup.value = updatedSelectedGroup
        }
    }

    fun selectAllDuplicates(select: Boolean) {
        val updatedGroups = _duplicateGroups.value.map { group ->
            val updatedFiles = group.files.mapIndexed { index, f ->
                if (select) f.copy(isSelected = index != 0) else f.copy(isSelected = false)
            }
            group.copy(files = updatedFiles)
        }
        _duplicateGroups.value = updatedGroups
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val groups = _duplicateGroups.value
            val context = getApplication<Application>().applicationContext
            var deletedCount = 0
            var freedBytes = 0L

            val remainingGroups = mutableListOf<DuplicateGroup>()

            for (group in groups) {
                val remainingFiles = mutableListOf<DuplicateFile>()
                for (file in group.files) {
                    if (file.isSelected) {
                        try {
                            if (file.path.isNotEmpty()) {
                                val f = File(file.path)
                                if (f.exists()) {
                                    f.delete()
                                }
                            }
                            if (file.uri != Uri.EMPTY) {
                                context.contentResolver.delete(file.uri, null, null)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        deletedCount++
                        freedBytes += file.size
                    } else {
                        remainingFiles.add(file)
                    }
                }
                if (remainingFiles.size > 1) {
                    remainingGroups.add(group.copy(files = remainingFiles))
                }
            }

            _duplicateGroups.value = remainingGroups
            _statusMessage.value = "Successfully deleted $deletedCount clone files and freed ${formatBytes(freedBytes)}!"

            if (_selectedGroup.value != null) {
                val updatedSelectedGroup = remainingGroups.find { it.hash == _selectedGroup.value!!.hash }
                if (updatedSelectedGroup == null || updatedSelectedGroup.files.size <= 1) {
                    _selectedGroup.value = null
                } else {
                    _selectedGroup.value = updatedSelectedGroup
                }
            }
        }
    }

    fun deleteFile(file: DuplicateFile) {
        viewModelScope.launch {
            val groups = _duplicateGroups.value
            val context = getApplication<Application>().applicationContext
            
            try {
                if (file.path.isNotEmpty()) {
                    val f = File(file.path)
                    if (f.exists()) f.delete()
                }
                if (file.uri != Uri.EMPTY) {
                    context.contentResolver.delete(file.uri, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val remainingGroups = groups.mapNotNull { group ->
                val remainingFiles = group.files.filter { it.id != file.id }
                if (remainingFiles.size > 1) group.copy(files = remainingFiles) else null
            }
            _duplicateGroups.value = remainingGroups
            
            if (_selectedGroup.value != null) {
                val updatedSelectedGroup = remainingGroups.find { it.hash == _selectedGroup.value!!.hash }
                if (updatedSelectedGroup == null || updatedSelectedGroup.files.size <= 1) {
                    _selectedGroup.value = null
                } else {
                    _selectedGroup.value = updatedSelectedGroup
                }
            }
            
            _statusMessage.value = "Successfully deleted file"
        }
    }

    fun shareFile(context: Context, file: DuplicateFile) {
        if (file.uri == Uri.EMPTY) return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, file.uri)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share File"))
    }

    fun shareSelectedFiles(context: Context) {
        val selectedUris = _duplicateGroups.value.flatMap { group ->
            group.files.filter { it.isSelected && it.uri != Uri.EMPTY }.map { it.uri }
        }.toCollection(ArrayList())

        if (selectedUris.isEmpty()) return

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, selectedUris)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Duplicates"))
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.getDefault(), "%.2f GB", gb)
    }
}
