package com.arfa_zuha.phonecleaner.ms321.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            Log.d("AnalyticsHelper", "Google Firebase Analytics initialized successfully!")
        } catch (e: Exception) {
            Log.e("AnalyticsHelper", "Failed to initialize Firebase Analytics: ${e.message}")
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
            Log.d("AnalyticsHelper", "Event logged: $eventName")
        } catch (e: Exception) {
            Log.e("AnalyticsHelper", "Failed to log event '$eventName': ${e.message}")
        }
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logCleanAction(actionType: String, sizeFreedBytes: Long = 0) {
        val bundle = Bundle().apply {
            putString("action_type", actionType)
            putLong("size_freed_bytes", sizeFreedBytes)
        }
        logEvent("clean_action_completed", bundle)
    }

    fun logAdImpression(adFormat: String) {
        val bundle = Bundle().apply {
            putString("ad_format", adFormat)
        }
        logEvent("ad_impression_recorded", bundle)
    }
}
