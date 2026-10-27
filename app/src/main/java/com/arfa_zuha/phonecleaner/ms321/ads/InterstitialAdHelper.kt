package com.arfa_zuha.phonecleaner.ms321.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    
    private const val AD_UNIT_ID = "ca-app-pub-8393457688147659/5382269245" // Real Production Interstitial Ad ID

    private val pendingWaiters = mutableListOf<(InterstitialAd?) -> Unit>()

    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) return
        
        val isPremium = try { BillingHelper.getInstance(context).isPremium.value } catch (e: Exception) { false }
        if (isPremium) return

        isLoading = true
        Log.d("AdMobInter", "Requesting Interstitial Ad...")
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdMobInter", "Interstitial Ad loaded successfully!")
                    interstitialAd = ad
                    isLoading = false
                    notifyWaiters(ad)
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdMobInter", "Interstitial Ad failed to load: code=${adError.code}, message=${adError.message}")
                    interstitialAd = null
                    isLoading = false
                    notifyWaiters(null)
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

    fun showAd(activity: Activity, onComplete: () -> Unit) {
        val isPremium = try { BillingHelper.getInstance(activity).isPremium.value } catch (e: Exception) { false }

        if (isPremium || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        val cachedAd = interstitialAd
        if (cachedAd != null) {
            interstitialAd = null
            displayAd(activity, cachedAd, onComplete)
            return
        }

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

        // Set to 4000ms so that real-world mobile latency (3-5s) doesn't cause false timeouts
        handler.postDelayed(timeoutRunnable, 4000L)

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

        if (!isLoading) {
            loadAd(activity.applicationContext)
        }
    }

    private fun displayAd(activity: Activity, ad: InterstitialAd, onComplete: () -> Unit) {
        AdLoadingDialogHelper.showLoadingAndThen(activity) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = false
                            onComplete()
                            loadAd(activity.applicationContext)
                        }
                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: AdError) {
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

    private var lastAdShowTime: Long = 0
    private const val AD_COOLDOWN_MS = 30 * 1000L

    fun showFileManagerAdWithCooldown(activity: Activity, onComplete: () -> Unit) {
        showAdWithCooldown(activity, AD_COOLDOWN_MS, onComplete)
    }

    fun showAdWithCooldown(activity: Activity, cooldownMs: Long = AD_COOLDOWN_MS, onComplete: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdShowTime >= cooldownMs) {
            showAd(activity) {
                lastAdShowTime = System.currentTimeMillis()
                onComplete()
            }
        } else {
            onComplete()
        }
    }
}
