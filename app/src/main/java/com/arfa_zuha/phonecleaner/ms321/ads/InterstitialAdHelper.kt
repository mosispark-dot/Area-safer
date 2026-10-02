package com.arfa_zuha.phonecleaner.ms321.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdHelper {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    
    // Sample Test Ad ID for Interstitial Ads
    private const val AD_UNIT_ID = "ca-app-pub-8393457688147659/5382269245" // Real Interstitial Ad ID

    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) return
        
        // Don't load if user is premium
        if (BillingHelper.getInstance(context).isPremium.value) return

        isLoading = true
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    // Retry loading after 3 seconds for network reliability
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadAd(context)
                    }, 3000L)
                }
            }
        )
    }

    /**
     * Shows an interstitial ad and runs [onComplete] when it's closed or if it fails.
     */
    fun showAd(activity: Activity, onComplete: () -> Unit) {
        val isPremium = BillingHelper.getInstance(activity).isPremium.value

        if (interstitialAd == null || isPremium || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            loadAd(activity)
            return
        }

        AdLoadingDialogHelper.showLoadingAndThen(activity) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed && interstitialAd != null) {
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            AdStateManager.isShowingFullScreenAd.value = false
                            onComplete()
                            loadAd(activity) // Pre-load next ad
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            AdStateManager.isShowingFullScreenAd.value = false
                            onComplete()
                            loadAd(activity)
                        }
                        override fun onAdShowedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = true
                        }
                    }
                    
                    interstitialAd?.show(activity)
                } else {
                    onComplete()
                }
            } catch (e: Exception) {
                onComplete()
            }
        }
    }

    private var lastFileManagerAdTime: Long = 0
    private const val FILE_MANAGER_AD_COOLDOWN_MS = 80 * 1000L // 80 seconds

    fun showFileManagerAdWithCooldown(activity: Activity, onComplete: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFileManagerAdTime >= FILE_MANAGER_AD_COOLDOWN_MS) {
            showAd(activity) {
                lastFileManagerAdTime = System.currentTimeMillis()
                onComplete()
            }
        } else {
            onComplete()
        }
    }
}
