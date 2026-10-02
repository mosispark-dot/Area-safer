package com.arfa_zuha.phonecleaner.ms321.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arfa_zuha.phonecleaner.ms321.data.AppInfoItem
import com.arfa_zuha.phonecleaner.ms321.data.BatteryInfo
import com.arfa_zuha.phonecleaner.ms321.data.BatteryRepository
import com.arfa_zuha.phonecleaner.ms321.data.CleanerRepository
import com.arfa_zuha.phonecleaner.ms321.data.CompressionRepository
import com.arfa_zuha.phonecleaner.ms321.data.CompressionResult
import com.arfa_zuha.phonecleaner.ms321.data.CompressionType
import com.arfa_zuha.phonecleaner.ms321.data.PowerMode
import com.arfa_zuha.phonecleaner.ms321.data.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class CleanerUiState(
    val isLoading: Boolean = false,
    val storageInfo: StorageInfo? = null,
    val installedApps: List<AppInfoItem> = emptyList(),
    val statusMessage: String? = null
)

data class CompressionUiState(
    val selectedType: CompressionType = CompressionType.PICTURE,
    val selectedUris: List<Uri> = emptyList(),
    val fileNames: List<String> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val qualityLevel: Float = 0.6f,
    val isCompressing: Boolean = false,
    val result: CompressionResult? = null
) {
    val estimatedBytesSaved: Long
        get() {
            if (totalSizeBytes <= 0L) return 0L
            val compressionRatio = when (selectedType) {
                CompressionType.PICTURE -> (1f - qualityLevel).coerceIn(0.1f, 0.8f)
                CompressionType.VIDEO -> (1f - qualityLevel).coerceIn(0.1f, 0.7f)
                CompressionType.AUDIO -> (1f - qualityLevel).coerceIn(0.1f, 0.6f)
                CompressionType.FILE -> 0.35f
            }
            return (totalSizeBytes * compressionRatio).toLong()
        }
}

data class BatteryUiState(
    val batteryInfo: BatteryInfo? = null,
    val selectedPowerMode: PowerMode = PowerMode.NORMAL,
    val isOptimizing: Boolean = false,
    val isOptimized: Boolean = false,
    val optimizationMessage: String? = null,
    val boostedMinutesGained: Int = 0
)

class CleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CleanerRepository(application)
    private val compressionRepository = CompressionRepository(application)
    private val batteryRepository = BatteryRepository(application)

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    private val _compressionState = MutableStateFlow(CompressionUiState())
    val compressionState: StateFlow<CompressionUiState> = _compressionState.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryUiState())
    val batteryState: StateFlow<BatteryUiState> = _batteryState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            val storage = repository.getStorageInfo()
            val apps = repository.getInstalledApps()
            val battery = batteryRepository.getBatteryInfo()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    storageInfo = storage,
                    installedApps = apps
                )
            }
            _batteryState.update { it.copy(batteryInfo = battery) }
        }
    }

    fun clearJunkFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Cleaning junk & temp files...") }
            val cleared = repository.clearJunkFiles()
            val storage = repository.getStorageInfo()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    storageInfo = storage,
                    statusMessage = "Successfully freed ${formatBytes(cleared)} of junk files!"
                )
            }
        }
    }

    // Battery Optimizer Functions
    fun refreshBatteryInfo() {
        viewModelScope.launch {
            val info = batteryRepository.getBatteryInfo()
            _batteryState.update { it.copy(batteryInfo = info) }
        }
    }

    fun setPowerMode(mode: PowerMode) {
        _batteryState.update { it.copy(selectedPowerMode = mode) }
    }

    fun optimizeBattery() {
        viewModelScope.launch {
            _batteryState.update { it.copy(isOptimizing = true, optimizationMessage = null) }
            val (boostedMins, msg) = batteryRepository.optimizeBattery()
            val updatedInfo = batteryRepository.getBatteryInfo()
            _batteryState.update {
                it.copy(
                    batteryInfo = updatedInfo,
                    isOptimizing = false,
                    isOptimized = true,
                    optimizationMessage = msg,
                    boostedMinutesGained = boostedMins
                )
            }
        }
    }

    // Compression Functions
    fun setCompressionType(type: CompressionType) {
        _compressionState.update {
            it.copy(
                selectedType = type,
                selectedUris = emptyList(),
                fileNames = emptyList(),
                totalSizeBytes = 0L,
                result = null
            )
        }
    }

    fun selectFilesForCompression(uris: List<Uri>) {
        val names = mutableListOf<String>()
        var totalSize = 0L
        for (uri in uris) {
            val (name, size) = compressionRepository.getFileNameAndSize(uri)
            names.add(name)
            totalSize += size
        }
        _compressionState.update {
            it.copy(
                selectedUris = uris,
                fileNames = names,
                totalSizeBytes = totalSize,
                result = null
            )
        }
    }

    fun setQualityLevel(level: Float) {
        _compressionState.update { it.copy(qualityLevel = level) }
    }

    fun performCompression() {
        val currentState = _compressionState.value
        val uris = currentState.selectedUris
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _compressionState.update { it.copy(isCompressing = true, result = null) }

            val res = when (currentState.selectedType) {
                CompressionType.PICTURE -> {
                    val qualityPercent = (currentState.qualityLevel * 100).toInt()
                    compressionRepository.compressPicturesBatch(uris, qualityPercent)
                }
                CompressionType.VIDEO -> {
                    compressionRepository.compressVideosBatch(uris, currentState.qualityLevel)
                }
                CompressionType.AUDIO -> {
                    compressionRepository.compressAudioBatch(uris, currentState.qualityLevel)
                }
                CompressionType.FILE -> {
                    compressionRepository.compressGenericFilesBatch(uris)
                }
            }

            _compressionState.update {
                it.copy(
                    isCompressing = false,
                    result = res
                )
            }
            refreshData()
        }
    }

    fun resetCompression() {
        _compressionState.update {
            CompressionUiState(selectedType = it.selectedType)
        }
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

    fun formatMinutesToHours(minutes: Int): String {
        val hrs = minutes / 60
        val mins = minutes % 60
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }
}
