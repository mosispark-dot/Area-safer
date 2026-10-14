package com.arfa_zuha.phonecleaner.ms321

import android.app.Application
import com.arfa_zuha.phonecleaner.ms321.ads.AppOpenAdManager
import com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig

class PhoneCleanerApp : Application() {
    
    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Analytics (Firebase)
        com.arfa_zuha.phonecleaner.ms321.analytics.AnalyticsHelper.init(this)

        // Initialize Google Play Billing to start querying purchases immediately
        BillingHelper.getInstance(this)

        // Initialize App Open Ad Manager IMMEDIATELY so it's never uninitialized
        appOpenAdManager = AppOpenAdManager(this)
        
        // Next-Gen GMA SDK Configuration
        val requestConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf("EMULATOR"))
            .build()
        MobileAds.setRequestConfiguration(requestConfig)

        val initConfig = InitializationConfig.Builder("ca-app-pub-8393457688147659~6995482245")
            .setRequestConfiguration(requestConfig)
            .build()

        // Initialize Next-Gen Mobile Ads SDK
        MobileAds.initialize(this, initConfig) {
            appOpenAdManager.loadAd(this)
            InterstitialAdHelper.loadAd(this)
        }
    }
}
