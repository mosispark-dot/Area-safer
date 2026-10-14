package com.arfa_zuha.phonecleaner.ms321.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

object InterstitialAdHelper {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    
    private const val AD_UNIT_ID = "ca-app-pub-8393457688147659/5382269245" // Real Interstitial Ad ID

    // Waiters queue: prevents missed impressions when user taps quickly before ad finishes loading
    private val pendingWaiters = mutableListOf<(InterstitialAd?) -> Unit>()

    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) return
        
        // Don't load if user is premium
        if (BillingHelper.getInstance(context).isPremium.value) return

        isLoading = true
        Log.d("AdMobInter", "Requesting Interstitial Ad from Next-Gen SDK...")
        val request = AdRequest.Builder(AD_UNIT_ID).build()
        InterstitialAd.load(
            request,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdMobInter", "Interstitial Ad loaded successfully into cache!")
                    interstitialAd = ad
                    isLoading = false
                    notifyWaiters(ad)
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdMobInter", "Interstitial Ad failed to load: code=${adError.code}, message=${adError.message}")
                    interstitialAd = null
                    isLoading = false
                    notifyWaiters(null)
                    // Retry with 15s delay, avoid aggressive looping
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadAd(context.applicationContext)
                    }, 15000L)
                }
            }
        )
    }

    private fun notifyWaiters(ad: InterstitialAd?) {
        if (pendingWaiters.isNotEmpty()) {
            val copy = ArrayList(pendingWaiters)
            pendingWaiters.clear()
            for (waiter in copy) {
                waiter.invoke(ad)
            }
        }
    }

    /**
     * Shows an interstitial ad and runs [onComplete] when it's closed or if it fails.
     * High Show Rate Optimization: If ad is currently loading, briefly displays the loading dialog
     * (up to 2.5s) to wait for the ad rather than dropping the impression opportunity.
     */
    fun showAd(activity: Activity, onComplete: () -> Unit) {
        val isPremium = BillingHelper.getInstance(activity).isPremium.value

        if (isPremium || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        // Case 1: Ad is already in memory -> show immediately!
        val cachedAd = interstitialAd
        if (cachedAd != null) {
            interstitialAd = null // Consume
            displayAd(activity, cachedAd, onComplete)
            return
        }

        // Case 2: Ad is loading or not cached yet -> Show loading dialog and wait up to 2.5s
        val dialog = AdLoadingDialogHelper.showLoadingDialog(activity)
        val handler = Handler(Looper.getMainLooper())
        var isResolved = false

        val timeoutRunnable = Runnable {
            if (!isResolved) {
                isResolved = true
                AdLoadingDialogHelper.dismissDialog(dialog)
                onComplete()
            }
        }

        // 2500ms safety timeout: if ad doesn't load within this window, proceed smoothly
        handler.postDelayed(timeoutRunnable, 2500L)

        val waiter: (InterstitialAd?) -> Unit = { ad ->
            if (!isResolved) {
                isResolved = true
                handler.removeCallbacks(timeoutRunnable)
                AdLoadingDialogHelper.dismissDialog(dialog)
                if (ad != null && !activity.isFinishing && !activity.isDestroyed) {
                    displayAd(activity, ad, onComplete)
                } else {
                    onComplete()
                }
            }
        }

        pendingWaiters.add(waiter)

        // If not already loading, trigger load now
        if (!isLoading) {
            loadAd(activity.applicationContext)
        }
    }

    private fun displayAd(activity: Activity, ad: InterstitialAd, onComplete: () -> Unit) {
        AdLoadingDialogHelper.showLoadingAndThen(activity) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    ad.adEventCallback = object : InterstitialAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = false
                            onComplete()
                            loadAd(activity.applicationContext) // Immediately preload next ad!
                        }
                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            Log.e("AdMobInter", "Failed to show: ${fullScreenContentError.message}")
                            AdStateManager.isShowingFullScreenAd.value = false
                            onComplete()
                            loadAd(activity.applicationContext)
                        }
                        override fun onAdShowedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = true
                        }
                    }
                    ad.show(activity)
                } else {
                    onComplete()
                    loadAd(activity.applicationContext)
                }
            } catch (e: Exception) {
                onComplete()
                loadAd(activity.applicationContext)
            }
        }
    }

    private var lastFileManagerAdTime: Long = 0
    private const val FILE_MANAGER_AD_COOLDOWN_MS = 60 * 1000L // 60 seconds

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
