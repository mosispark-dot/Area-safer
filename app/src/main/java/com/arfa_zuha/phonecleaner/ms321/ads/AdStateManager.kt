package com.arfa_zuha.phonecleaner.ms321.ads

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Coordinates ad states across the app to prevent collisions.
 * Master switch allows commenting out/disabling all ads during UI design phase.
 */
object AdStateManager {
    // When true, disables ads during UI development. Enabled (false) now for testing with official test ads.
    const val ADS_TEMPORARILY_COMMENTED_OUT = false

    // True when a full-screen interstitial ad is displaying
    val isShowingFullScreenAd = MutableStateFlow(false)
}
