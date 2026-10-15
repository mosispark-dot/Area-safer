package com.arfa_zuha.phonecleaner.ms321

import android.app.Application
import com.arfa_zuha.phonecleaner.ms321.ads.AppOpenAdManager
import com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.gms.ads.MobileAds

class PhoneCleanerApp : Application() {
    
    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Analytics (Firebase)
        try {
            com.arfa_zuha.phonecleaner.ms321.analytics.AnalyticsHelper.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize Google Play Billing to start querying purchases immediately
        try {
            BillingHelper.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize App Open Ad Manager IMMEDIATELY so it's never uninitialized
        appOpenAdManager = AppOpenAdManager(this)

        // Initialize Official Production AdMob SDK safely
        try {
            MobileAds.initialize(this) {
                appOpenAdManager.loadAd(this)
                InterstitialAdHelper.loadAd(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
