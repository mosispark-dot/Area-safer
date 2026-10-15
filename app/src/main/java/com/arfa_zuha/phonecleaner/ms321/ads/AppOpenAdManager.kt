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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val application: Application) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isColdStart = true

    private val AD_UNIT_ID = "ca-app-pub-8393457688147659/4369318908" // Real Production App Open Ad ID

    private val pendingWaiters = mutableListOf<(AppOpenAd?) -> Unit>()
    
    init {
        application.registerActivityLifecycleCallbacks(this)
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    if (!isColdStart) {
                        showAdIfAvailable()
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        Log.d("AdMobAppOpen", "Requesting App Open Ad...")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d("AdMobAppOpen", "App Open Ad loaded successfully!")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    notifyWaiters(ad)
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMobAppOpen", "App Open Ad failed to load: code=${loadAdError.code}, message=${loadAdError.message}")
                    isLoadingAd = false
                    notifyWaiters(null)
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
        val isPremium = try { BillingHelper.getInstance(application).isPremium.value } catch (e: Exception) { false }
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

    fun fetchAdAndShow(activity: Activity, onComplete: () -> Unit) {
        isColdStart = false

        val isPremium = try { BillingHelper.getInstance(application).isPremium.value } catch (e: Exception) { false }
        if (isPremium || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        if (isAdAvailable()) {
            showAdNow(activity, onComplete)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var isResolved = false

        val timeoutRunnable = Runnable {
            if (!isResolved) {
                isResolved = true
                onComplete()
            }
        }
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
        appOpenAd = null

        AdLoadingDialogHelper.showLoadingAndThen(activity) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed && adToShow != null) {
                    adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = false
                            loadAd(application)
                            onComplete()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AdMobAppOpen", "Failed to show: ${adError.message}")
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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
