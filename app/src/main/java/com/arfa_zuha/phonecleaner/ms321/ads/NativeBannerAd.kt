package com.arfa_zuha.phonecleaner.ms321.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

const val BANNER_AD_UNIT_ID = "ca-app-pub-8393457688147659/4069936000"
const val NATIVE_ADVANCE_AD_UNIT_ID = "ca-app-pub-8393457688147659/5682400578"

@Composable
fun NativeBannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val billingHelper = remember(context) {
        try {
            BillingHelper.getInstance(context)
        } catch (e: Exception) {
            null
        }
    }
    val isPremiumState = billingHelper?.isPremium?.collectAsState()
    val isPremium = isPremiumState?.value ?: false

    if (isPremium) {
        return
    }

    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                adViewRef?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        adViewRef = this
                        setAdSize(AdSize.BANNER)
                        adUnitId = BANNER_AD_UNIT_ID
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("AdMobBanner", "Banner Ad loaded successfully!")
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e("AdMobBanner", "Banner Ad failed to load: code=${error.code}, message=${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

@Composable
fun MediumRectangleAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val billingHelper = remember(context) {
        try {
            BillingHelper.getInstance(context)
        } catch (e: Exception) {
            null
        }
    }
    val isPremiumState = billingHelper?.isPremium?.collectAsState()
    val isPremium = isPremiumState?.value ?: false

    if (isPremium) {
        return
    }

    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                adViewRef?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        adViewRef = this
                        setAdSize(AdSize.MEDIUM_RECTANGLE)
                        adUnitId = NATIVE_ADVANCE_AD_UNIT_ID
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("AdMobMREC", "Medium Rectangle Ad loaded successfully!")
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e("AdMobMREC", "Medium Rectangle Ad failed: code=${error.code}, message=${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
