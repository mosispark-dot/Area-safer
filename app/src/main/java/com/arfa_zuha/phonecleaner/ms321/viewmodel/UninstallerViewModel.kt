package com.arfa_zuha.phonecleaner.ms321.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arfa_zuha.phonecleaner.ms321.data.ApkFileItem
import com.arfa_zuha.phonecleaner.ms321.data.AppSortOption
import com.arfa_zuha.phonecleaner.ms321.data.InstalledAppItem
import com.arfa_zuha.phonecleaner.ms321.data.UninstallerRepository
import com.arfa_zuha.phonecleaner.ms321.data.UninstallerTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class UninstallerUiState(
    val selectedTab: UninstallerTab = UninstallerTab.INSTALLED_APPS,
    val installedApps: List<InstalledAppItem> = emptyList(),
    val apkFiles: List<ApkFileItem> = emptyList(),
    val searchQuery: String = "",
    val sortOption: AppSortOption = AppSortOption.SIZE,
    val showSystemApps: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

class UninstallerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UninstallerRepository(application)

    private val _uiState = MutableStateFlow(UninstallerUiState())
    val uiState: StateFlow<UninstallerUiState> = _uiState.asStateFlow()

    init {
        scanAll()
    }

    fun scanAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            val apps = repository.getInstalledApps()
            val apks = repository.scanApkFiles()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    installedApps = apps,
                    apkFiles = apks
                )
            }
        }
    }

    fun setTab(tab: UninstallerTab) {
        _uiState.update { it.copy(selectedTab = tab, searchQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOption(option: AppSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun toggleSystemApps(show: Boolean) {
        _uiState.update { it.copy(showSystemApps = show) }
    }

    fun toggleAppSelection(packageName: String) {
        val updatedApps = _uiState.value.installedApps.map { app ->
            if (app.packageName == packageName) app.copy(isSelected = !app.isSelected) else app
        }
        _uiState.update { it.copy(installedApps = updatedApps) }
    }

    fun toggleApkSelection(path: String) {
        val updatedApks = _uiState.value.apkFiles.map { apk ->
            if (apk.path == path) apk.copy(isSelected = !apk.isSelected) else apk
        }
        _uiState.update { it.copy(apkFiles = updatedApks) }
    }

    fun selectAllApps(select: Boolean) {
        val updatedApps = _uiState.value.installedApps.map { it.copy(isSelected = select) }
        _uiState.update { it.copy(installedApps = updatedApps) }
    }

    fun selectAllApks(select: Boolean) {
        val updatedApks = _uiState.value.apkFiles.map { it.copy(isSelected = select) }
        _uiState.update { it.copy(apkFiles = updatedApks) }
    }

    fun deleteSelectedApks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            val selectedPaths = _uiState.value.apkFiles.filter { it.isSelected }.map { it.path }
            val (count, freedBytes) = repository.deleteApkFiles(selectedPaths)
            val updatedApks = repository.scanApkFiles()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    apkFiles = updatedApks,
                    statusMessage = "Successfully deleted $count APK files and freed ${formatBytes(freedBytes)} storage!"
                )
            }
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val packageUri = Uri.fromParts("package", packageName, null)
            val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri).apply {
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(uninstallIntent)
        } catch (e: Exception) {
            try {
                val packageUri = Uri.parse("package:$packageName")
                @Suppress("DEPRECATION")
                val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(uninstallIntent)
            } catch (ex: Exception) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }
        }
    }

    fun uninstallSelectedApps(context: Context) {
        val selectedPackages = _uiState.value.installedApps.filter { it.isSelected }.map { it.packageName }
        for (pkgName in selectedPackages) {
            uninstallApp(context, pkgName)
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
}
