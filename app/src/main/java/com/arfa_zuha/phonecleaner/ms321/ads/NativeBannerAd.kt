package com.arfa_zuha.phonecleaner.ms321.ads

import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

// Real Production AdMob Ad Unit IDs
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
            .wrapContentHeight()
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
                        val displayMetrics = ctx.resources.displayMetrics
                        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
                        val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)
                        setAdSize(adaptiveSize)
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

/**
 * MediumRectangleAd Composable:
 * Correctly loads the Native Advance Ad Unit using AdLoader + NativeAdView.
 * If Native inventory is filling, automatically falls back to Banner AdView so the slot is never blank.
 */
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

    var isLoaded by remember { mutableStateOf(false) }
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var fallbackAdView by remember { mutableStateOf<AdView?>(null) }

    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    DisposableEffect(Unit) {
        onDispose {
            try {
                loadedNativeAd?.destroy()
                fallbackAdView?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                factory = { ctx ->
                    val density = ctx.resources.displayMetrics.density
                    fun dpToPx(dp: Int): Int = (dp * density).toInt()

                    val container = FrameLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val nativeAdView = NativeAdView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val rootLayout = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // 1. Top "Advertisement" Pill Badge (matching App Open native style)
                    val adBadge = TextView(ctx).apply {
                        text = "Advertisement"
                        textSize = 9.5f
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(3))
                        background = GradientDrawable().apply {
                            cornerRadius = dpToPx(10).toFloat()
                            setColor(Color.parseColor("#8E8E93")) // Clean Gray Pill Badge
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dpToPx(8)
                        }
                    }
                    rootLayout.addView(adBadge)

                    // 2. Large Bold Headline
                    val adHeadline = TextView(ctx).apply {
                        textSize = 18f
                        setTypeface(null, Typeface.BOLD)
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        setTextColor(Color.parseColor("#1F2937"))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dpToPx(4)
                        }
                    }
                    rootLayout.addView(adHeadline)

                    // 3. Subtitle / Advertiser / Body
                    val adBody = TextView(ctx).apply {
                        textSize = 13f
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        setTextColor(Color.parseColor("#6B7280"))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dpToPx(10)
                        }
                    }
                    rootLayout.addView(adBody)

                    // Hidden icon view for binding compatibility
                    val adIconView = ImageView(ctx).apply {
                        visibility = View.GONE
                        layoutParams = LinearLayout.LayoutParams(0, 0)
                    }
                    rootLayout.addView(adIconView)

                    // 4. Large Rich MediaView (Video / Image Banner Preview)
                    val adMediaView = MediaView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(180)
                        ).apply {
                            bottomMargin = dpToPx(12)
                        }
                        clipToOutline = true
                        outlineProvider = object : ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: Outline) {
                                outline.setRoundRect(0, 0, view.width, view.height, dpToPx(10).toFloat())
                            }
                        }
                    }
                    rootLayout.addView(adMediaView)

                    // 5. Full Width Call to Action Button ("MORE" / "INSTALL")
                    val adCallToAction = Button(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(42)
                        )
                        textSize = 13.5f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        background = GradientDrawable().apply {
                            cornerRadius = dpToPx(8).toFloat()
                            setColor(Color.parseColor("#374151")) // Dark Charcoal Button matching reference
                        }
                    }
                    rootLayout.addView(adCallToAction)

                    nativeAdView.addView(rootLayout)

                    // Bind views to NativeAdView
                    nativeAdView.headlineView = adHeadline
                    nativeAdView.bodyView = adBody
                    nativeAdView.callToActionView = adCallToAction
                    nativeAdView.iconView = adIconView
                    nativeAdView.mediaView = adMediaView

                    container.addView(nativeAdView)

                    // Fallback to Banner AdView if Native ad request encounters no fill
                    fun loadFallbackBanner() {
                        try {
                            val adView = AdView(ctx).apply {
                                fallbackAdView = this
                                val displayMetrics = ctx.resources.displayMetrics
                                val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
                                val fallbackSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)
                                setAdSize(fallbackSize)
                                adUnitId = BANNER_AD_UNIT_ID
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        isLoaded = true
                                        container.removeAllViews()
                                        container.addView(this@apply)
                                        Log.d("AdMobMREC", "Fallback Medium Rectangle Banner loaded successfully!")
                                    }
                                    override fun onAdFailedToLoad(err: LoadAdError) {
                                        Log.e("AdMobMREC", "Fallback Banner failed: code=${err.code}, message=${err.message}")
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Load Native Advance Ad with AdLoader
                    val adLoader = AdLoader.Builder(ctx, NATIVE_ADVANCE_AD_UNIT_ID)
                        .forNativeAd { nativeAd ->
                            loadedNativeAd?.destroy()
                            loadedNativeAd = nativeAd

                            // Populate Headline
                            adHeadline.text = nativeAd.headline ?: ""

                            // Populate Body / Advertiser
                            if (!nativeAd.body.isNullOrEmpty()) {
                                adBody.text = nativeAd.body
                                adBody.visibility = View.VISIBLE
                            } else if (!nativeAd.advertiser.isNullOrEmpty()) {
                                adBody.text = nativeAd.advertiser
                                adBody.visibility = View.VISIBLE
                            } else {
                                adBody.visibility = View.GONE
                            }

                            // Populate Call To Action (Uppercase like in reference)
                            if (!nativeAd.callToAction.isNullOrEmpty()) {
                                adCallToAction.text = nativeAd.callToAction?.uppercase()
                                adCallToAction.visibility = View.VISIBLE
                            } else {
                                adCallToAction.text = "MORE"
                                adCallToAction.visibility = View.VISIBLE
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
                                adMediaView.visibility = View.VISIBLE
                            } else {
                                adMediaView.visibility = View.GONE
                            }

                            // Register the NativeAd object
                            nativeAdView.setNativeAd(nativeAd)
                            isLoaded = true
                            Log.d("AdMobNative", "Native Advance Ad loaded and rendered successfully!")
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e("AdMobNative", "Native Advance Ad failed: code=${error.code}, message=${error.message}. Triggering fallback banner...")
                                loadFallbackBanner()
                            }
                        })
                        .withNativeAdOptions(
                            NativeAdOptions.Builder()
                                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                                .build()
                        )
                        .build()

                    adLoader.loadAd(AdRequest.Builder().build())

                    container
                }
            )
        }
    }
}
