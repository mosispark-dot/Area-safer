package com.arfa_zuha.phonecleaner.ms321

import android.app.Application
import com.arfa_zuha.phonecleaner.ms321.ads.AppOpenAdManager
import com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds

class PhoneCleanerApp : Application() {
    
    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Play Billing to start querying purchases immediately
        BillingHelper.getInstance(this)

        // Initialize App Open Ad Manager IMMEDIATELY so it's never uninitialized
        appOpenAdManager = AppOpenAdManager(this)
        
        // Initialize AdMob Test Devices Configuration
        val requestConfiguration = MobileAds.getRequestConfiguration()
            .toBuilder()
            .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        // Initialize AdMob and pre-load ads inside completion callback
        MobileAds.initialize(this) {
            appOpenAdManager.loadAd(this)
            InterstitialAdHelper.loadAd(this)
        }
    }
}
