package com.arfa_zuha.phonecleaner.ms321.ads

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Coordinates ad states across the app to prevent collisions.
 * Inspired by airdeck's collision prevention mechanism.
 */
object AdStateManager {
    // True when a full-screen interstitial ad is displaying
    val isShowingFullScreenAd = MutableStateFlow(false)
}
