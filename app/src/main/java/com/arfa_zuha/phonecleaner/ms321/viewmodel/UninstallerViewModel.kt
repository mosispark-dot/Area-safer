package com.arfa_zuha.phonecleaner.ms321.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arfa_zuha.phonecleaner.ms321.data.ApkFileItem
import com.arfa_zuha.phonecleaner.ms321.data.AppSortOption
import com.arfa_zuha.phonecleaner.ms321.data.InstalledAppItem
import com.arfa_zuha.phonecleaner.ms321.data.UninstallerRepository
import com.arfa_zuha.phonecleaner.ms321.data.UninstallerTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
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

    // Thread-safe set of package names currently being uninstalled
    val pendingUninstallPackages: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    // Queue for batch uninstallation
    private val uninstallQueue = Collections.synchronizedList(mutableListOf<String>())

    init {
        scanAll(showLoading = true)
    }

    fun scanAll(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            }
            val pm = getApplication<Application>().packageManager
            val rawApps = repository.getInstalledApps()
            val apps = rawApps.filter { app ->
                try {
                    pm.getPackageInfo(app.packageName, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }
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

    /**
     * Instantly removes an uninstalled package from the UI list.
     * Called when BroadcastReceiver catches ACTION_PACKAGE_REMOVED or ACTION_PACKAGE_FULLY_REMOVED.
     */
    fun onPackageRemoved(packageName: String, context: Context? = null) {
        pendingUninstallPackages.remove(packageName)
        _uiState.update { state ->
            state.copy(
                installedApps = state.installedApps.filter { it.packageName != packageName },
                statusMessage = "Uninstalled $packageName"
            )
        }
        
        // If batch uninstalling, process next app in queue
        if (context != null && uninstallQueue.isNotEmpty()) {
            viewModelScope.launch {
                delay(300)
                processNextInUninstallQueue(context)
            }
        }
    }

    /**
     * Checks if any pending uninstalled packages have finished unregistering in PackageManager.
     * Performs silent background verification without showing full-screen loading spinners.
     */
    fun checkPendingUninstall(context: Context) {
        viewModelScope.launch {
            val pm = context.packageManager
            verifyPendingPackages(pm)
            delay(500)
            verifyPendingPackages(pm)
            
            if (uninstallQueue.isNotEmpty()) {
                processNextInUninstallQueue(context)
            } else {
                scanAll(showLoading = false) // Silent background refresh
            }
        }
    }

    private fun verifyPendingPackages(pm: PackageManager) {
        val removed = mutableListOf<String>()
        val currentPending = synchronized(pendingUninstallPackages) { pendingUninstallPackages.toList() }
        for (pkg in currentPending) {
            val isStillInstalled = try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            if (!isStillInstalled) {
                removed.add(pkg)
            }
        }
        if (removed.isNotEmpty()) {
            pendingUninstallPackages.removeAll(removed.toSet())
            _uiState.update { state ->
                state.copy(
                    installedApps = state.installedApps.filter { it.packageName !in removed }
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
            val selectedPaths = _uiState.value.apkFiles.filter { it.isSelected }.map { it.path }
            if (selectedPaths.isEmpty()) return@launch

            // Immediate optimistic UI update: instantly remove from list
            _uiState.update { state ->
                state.copy(
                    apkFiles = state.apkFiles.filter { it.path !in selectedPaths }
                )
            }

            val (count, freedBytes) = repository.deleteApkFiles(selectedPaths)

            _uiState.update {
                it.copy(
                    statusMessage = if (count > 0) "Successfully deleted $count APK file(s) and freed ${formatBytes(freedBytes)}!" else "Could not delete selected APK file(s)"
                )
            }
        }
    }

    fun deleteSingleApk(path: String) {
        viewModelScope.launch {
            // Immediate optimistic UI update: instantly remove from list
            _uiState.update { state ->
                state.copy(apkFiles = state.apkFiles.filter { it.path != path })
            }

            val (count, freedBytes) = repository.deleteApkFiles(listOf(path))
            _uiState.update {
                it.copy(
                    statusMessage = if (count > 0) "Deleted 1 APK file (${formatBytes(freedBytes)})" else "Failed to delete APK file"
                )
            }
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        pendingUninstallPackages.add(packageName)
        
        // Optimistic UI update: remove package from current list immediately
        _uiState.update { state ->
            state.copy(installedApps = state.installedApps.filter { it.packageName != packageName })
        }

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
        if (selectedPackages.isEmpty()) return

        uninstallQueue.clear()
        uninstallQueue.addAll(selectedPackages)

        processNextInUninstallQueue(context)
    }

    fun processNextInUninstallQueue(context: Context) {
        synchronized(uninstallQueue) {
            if (uninstallQueue.isNotEmpty()) {
                val nextPackage = uninstallQueue.removeAt(0)
                uninstallApp(context, nextPackage)
            }
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
