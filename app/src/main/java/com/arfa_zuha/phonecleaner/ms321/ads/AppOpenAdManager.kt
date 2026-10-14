package com.arfa_zuha.phonecleaner.ms321.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import java.util.Date

class AppOpenAdManager(private val application: Application) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isColdStart = true

    private val AD_UNIT_ID = "ca-app-pub-8393457688147659/4369318908" // Real App Open Ad ID

    // Queue of waiters to prevent duplicate ad requests
    private val pendingWaiters = mutableListOf<(AppOpenAd?) -> Unit>()
    
    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Ignore initial ON_START because SplashScreen handles cold start
                if (!isColdStart) {
                    showAdIfAvailable()
                }
            }
        })
    }

    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        Log.d("AdMobAppOpen", "Requesting App Open Ad from Next-Gen SDK...")
        val request = AdRequest.Builder(AD_UNIT_ID).build()
        AppOpenAd.load(
            request,
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d("AdMobAppOpen", "App Open Ad loaded successfully!")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    notifyWaiters(ad)
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("AdMobAppOpen", "App Open Ad failed to load: code=${adError.code}, message=${adError.message}")
                    isLoadingAd = false
                    notifyWaiters(null)
                    // Retry with 15s delay, avoiding aggressive looping
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadAd(context.applicationContext)
                    }, 15000L)
                }
            }
        )
    }

    private fun notifyWaiters(ad: AppOpenAd?) {
        if (pendingWaiters.isNotEmpty()) {
            val copy = ArrayList(pendingWaiters)
            pendingWaiters.clear()
            for (waiter in copy) {
                waiter.invoke(ad)
            }
        }
    }

    fun showAdIfAvailable() {
        val isPremium = BillingHelper.getInstance(application).isPremium.value
        val isShowingInter = AdStateManager.isShowingFullScreenAd.value

        if (!isAdAvailable() || isPremium || isShowingInter) {
            if (!isAdAvailable() && !isLoadingAd) {
                loadAd(application)
            }
            return
        }

        currentActivity?.let { activity ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                showAdNow(activity) {}
            }
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /**
     * High Show Rate Optimization for Splash:
     * Waits for existing in-flight ad rather than spawning redundant requests.
     */
    fun fetchAdAndShow(activity: Activity, onComplete: () -> Unit) {
        isColdStart = false // Cold start handled by splash

        val isPremium = BillingHelper.getInstance(application).isPremium.value
        if (isPremium || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        // Case 1: Ad is already available -> show it
        if (isAdAvailable()) {
            showAdNow(activity, onComplete)
            return
        }

        // Case 2: Ad is loading or needs to load -> Wait with timeout
        val handler = Handler(Looper.getMainLooper())
        var isResolved = false

        val timeoutRunnable = Runnable {
            if (!isResolved) {
                isResolved = true
                onComplete()
            }
        }
        // Wait up to 3.5s for ad
        handler.postDelayed(timeoutRunnable, 3500L)

        pendingWaiters.add { loadedAd ->
            if (!isResolved) {
                isResolved = true
                handler.removeCallbacks(timeoutRunnable)
                if (loadedAd != null && !activity.isFinishing && !activity.isDestroyed) {
                    showAdNow(activity, onComplete)
                } else {
                    onComplete()
                }
            }
        }

        // If not already loading, trigger load
        if (!isLoadingAd) {
            loadAd(application)
        }
    }

    private fun showAdNow(activity: Activity, onComplete: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        val adToShow = appOpenAd
        appOpenAd = null // Consume immediately

        AdLoadingDialogHelper.showLoadingAndThen(activity) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed && adToShow != null) {
                    adToShow.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = false
                            loadAd(application) // Preload next immediately
                            onComplete()
                        }
                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            Log.e("AdMobAppOpen", "Failed to show: ${fullScreenContentError.message}")
                            AdStateManager.isShowingFullScreenAd.value = false
                            loadAd(application)
                            onComplete()
                        }
                        override fun onAdShowedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = true
                        }
                    }
                    adToShow.show(activity)
                } else {
                    onComplete()
                    loadAd(application)
                }
            } catch (e: Exception) {
                onComplete()
                loadAd(application)
            }
        }
    }

    // ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
