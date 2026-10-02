package com.arfa_zuha.phonecleaner.ms321.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arfa_zuha.phonecleaner.ms321.data.AppUsageRepository
import com.arfa_zuha.phonecleaner.ms321.data.UsageSummary
import com.arfa_zuha.phonecleaner.ms321.data.UsageTimeFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AppUsageViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedTimeFrame = MutableStateFlow(UsageTimeFrame.TODAY)
    val selectedTimeFrame: StateFlow<UsageTimeFrame> = _selectedTimeFrame

    private val _usageSummary = MutableStateFlow<UsageSummary?>(null)
    val usageSummary: StateFlow<UsageSummary?> = _usageSummary

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val granted = AppUsageRepository.hasUsagePermission(getApplication())
        _hasPermission.value = granted
        loadUsageStats(_selectedTimeFrame.value)
    }

    fun setTimeFrame(timeFrame: UsageTimeFrame) {
        _selectedTimeFrame.value = timeFrame
        loadUsageStats(timeFrame)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadUsageStats(timeFrame: UsageTimeFrame) {
        viewModelScope.launch {
            _isLoading.value = true
            val context = getApplication<Application>().applicationContext
            val summary = AppUsageRepository.getAppUsageStats(context, timeFrame)
            _usageSummary.value = summary
            _isLoading.value = false
        }
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0m"
        val totalMinutes = millis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
