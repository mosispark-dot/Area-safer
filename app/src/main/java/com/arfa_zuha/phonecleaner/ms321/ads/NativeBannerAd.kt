package com.arfa_zuha.phonecleaner.ms321.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

// Real Production AdMob Ad Unit IDs
const val BANNER_AD_UNIT_ID = "ca-app-pub-8393457688147659/4069936000"
const val NATIVE_ADVANCE_AD_UNIT_ID = "ca-app-pub-8393457688147659/5682400578"

@Composable
fun NativeBannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPremium by BillingHelper.getInstance(context).isPremium.collectAsState()

    if (isPremium) {
        return
    }

    var isLoaded by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            adViewRef?.destroy()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        adViewRef = this
                        
                        // Anchored Adaptive Banner
                        val displayMetrics = ctx.resources.displayMetrics
                        val density = displayMetrics.density
                        val adWidthPixels = displayMetrics.widthPixels.toFloat()
                        val adWidth = (adWidthPixels / density).toInt()
                        val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)

                        val bannerAdRequest = BannerAdRequest.Builder(BANNER_AD_UNIT_ID, adaptiveSize).build()
                        loadAd(
                            bannerAdRequest,
                            object : AdLoadCallback<BannerAd> {
                                override fun onAdLoaded(ad: BannerAd) {
                                    isLoaded = true
                                    Log.d("AdMobBanner", "Banner Ad loaded successfully! Size: $adaptiveSize")
                                }
                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    isLoaded = false
                                    Log.e("AdMobBanner", "Banner Ad failed to load: code=${adError.code}, message=${adError.message}")
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}

/**
 * Native Advance Ad Composable (replaces MediumRectangleAd with true Native Ad).
 * Functions seamlessly in Dashboard and Compression screens.
 */
@Composable
fun MediumRectangleAd(
    modifier: Modifier = Modifier
) {
    NativeAdvanceAd(modifier = modifier)
}

@Composable
fun NativeAdvanceAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPremium by BillingHelper.getInstance(context).isPremium.collectAsState()

    if (isPremium) {
        return
    }

    var isLoaded by remember { mutableStateOf(false) }
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }

    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    DisposableEffect(Unit) {
        onDispose {
            loadedNativeAd?.destroy()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                factory = { ctx ->
                    val density = ctx.resources.displayMetrics.density
                    fun dpToPx(dp: Int): Int = (dp * density).toInt()

                    val nativeAdView = NativeAdView(ctx)

                    val rootContainer = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Top header row: Icon + (Ad Badge + Headline) + Body
                    val topRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val adIconView = ImageView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42)).apply {
                            rightMargin = dpToPx(10)
                        }
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }

                    val textColumn = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val headlineRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val adBadge = TextView(ctx).apply {
                        text = "Ad"
                        textSize = 10f
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        setPadding(dpToPx(4), dpToPx(1), dpToPx(4), dpToPx(1))
                        background = GradientDrawable().apply {
                            cornerRadius = dpToPx(4).toFloat()
                            setColor(Color.parseColor("#E65100")) // Amber Ad Badge
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            rightMargin = dpToPx(6)
                        }
                    }

                    val adHeadline = TextView(ctx).apply {
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        setTextColor(onSurfaceColor)
                    }

                    headlineRow.addView(adBadge)
                    headlineRow.addView(adHeadline)

                    val adBody = TextView(ctx).apply {
                        textSize = 12f
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        setTextColor(onSurfaceVariantColor)
                        setPadding(0, dpToPx(2), 0, 0)
                    }

                    textColumn.addView(headlineRow)
                    textColumn.addView(adBody)

                    topRow.addView(adIconView)
                    topRow.addView(textColumn)
                    rootContainer.addView(topRow)

                    // MediaView
                    val adMediaView = MediaView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(150)
                        ).apply {
                            topMargin = dpToPx(8)
                            bottomMargin = dpToPx(8)
                        }
                    }
                    rootContainer.addView(adMediaView)

                    // Call to Action Button
                    val adCallToAction = Button(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(44)
                        )
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        background = GradientDrawable().apply {
                            cornerRadius = dpToPx(12).toFloat()
                            setColor(primaryColor)
                        }
                    }
                    rootContainer.addView(adCallToAction)

                    nativeAdView.addView(rootContainer)

                    // Bind views to NativeAdView
                    nativeAdView.headlineView = adHeadline
                    nativeAdView.bodyView = adBody
                    nativeAdView.callToActionView = adCallToAction
                    nativeAdView.iconView = adIconView

                    // Load the Native Ad with Next-Gen NativeAdLoader
                    val nativeRequest = NativeAdRequest.Builder(
                        NATIVE_ADVANCE_AD_UNIT_ID,
                        listOf(NativeAd.NativeAdType.NATIVE)
                    )
                        .setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT)
                        .build()

                    NativeAdLoader.load(
                        nativeRequest,
                        object : NativeAdLoaderCallback {
                            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                                loadedNativeAd?.destroy()
                                loadedNativeAd = nativeAd

                                // Populate Headline
                                adHeadline.text = nativeAd.headline ?: ""

                                // Populate Body
                                if (!nativeAd.body.isNullOrEmpty()) {
                                    adBody.text = nativeAd.body
                                    adBody.visibility = View.VISIBLE
                                } else {
                                    adBody.visibility = View.GONE
                                }

                                // Populate Call To Action
                                if (!nativeAd.callToAction.isNullOrEmpty()) {
                                    adCallToAction.text = nativeAd.callToAction
                                    adCallToAction.visibility = View.VISIBLE
                                } else {
                                    adCallToAction.visibility = View.GONE
                                }

                                // Populate Icon
                                if (nativeAd.icon != null) {
                                    adIconView.setImageDrawable(nativeAd.icon?.drawable)
                                    adIconView.visibility = View.VISIBLE
                                } else {
                                    adIconView.visibility = View.GONE
                                }

                                // Populate MediaView
                                if (nativeAd.mediaContent != null) {
                                    adMediaView.mediaContent = nativeAd.mediaContent
                                }

                                // Register the NativeAd object
                                nativeAdView.registerNativeAd(nativeAd, adMediaView)
                                isLoaded = true
                                Log.d("AdMobNative", "Native Advance Ad loaded successfully!")
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                isLoaded = false
                                Log.e("AdMobNative", "Native Advance Ad failed: code=${adError.code}, message=${adError.message}")
                            }
                        }
                    )

                    nativeAdView
                }
            )
        }
    }
}
