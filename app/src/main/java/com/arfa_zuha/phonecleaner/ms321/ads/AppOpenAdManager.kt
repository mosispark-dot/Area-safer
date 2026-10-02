package com.arfa_zuha.phonecleaner.ms321.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    // Sample Test Ad ID for App Open Ads
    private val AD_UNIT_ID = "ca-app-pub-8393457688147659/4369318908" // Real App Open Ad ID
    
    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                showAdIfAvailable()
            }
        })
    }

    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadAd(context)
                    }, 3000L)
                }
            }
        )
    }

    fun showAdIfAvailable() {
        val isPremium = BillingHelper.getInstance(application).isPremium.value
        val isShowingInter = AdStateManager.isShowingFullScreenAd.value

        if (!isAdAvailable() || isPremium || isShowingInter) {
            loadAd(application)
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
        val isPremium = BillingHelper.getInstance(application).isPremium.value

        if (isPremium || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        if (isAdAvailable()) {
            showAdNow(activity, onComplete)
        } else {
            isLoadingAd = true
            var isTimeout = false
            val request = AdRequest.Builder().build()
            
            AppOpenAd.load(
                activity,
                AD_UNIT_ID,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        if (!isTimeout && !activity.isFinishing && !activity.isDestroyed) {
                            showAdNow(activity, onComplete)
                        } else if (!isTimeout) {
                            onComplete()
                        }
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        if (!isTimeout) {
                            onComplete()
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAd(application)
                        }, 3000L)
                    }
                }
            )
            
            // Safety timeout: if ad doesn't load in 4 seconds, proceed anyway
            Handler(Looper.getMainLooper()).postDelayed({
                if (isLoadingAd && !isTimeout) {
                    isTimeout = true
                    onComplete()
                }
            }, 4000L)
        }
    }

    private fun showAdNow(activity: Activity, onComplete: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }
        AdLoadingDialogHelper.showLoadingAndThen(activity) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed && appOpenAd != null) {
                    appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            appOpenAd = null
                            AdStateManager.isShowingFullScreenAd.value = false
                            loadAd(application)
                            onComplete()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            appOpenAd = null
                            AdStateManager.isShowingFullScreenAd.value = false
                            loadAd(application)
                            onComplete()
                        }
                        override fun onAdShowedFullScreenContent() {
                            AdStateManager.isShowingFullScreenAd.value = true
                        }
                    }
                    appOpenAd?.show(activity)
                } else {
                    onComplete()
                }
            } catch (e: Exception) {
                onComplete()
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
