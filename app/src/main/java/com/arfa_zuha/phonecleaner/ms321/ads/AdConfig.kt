package com.arfa_zuha.phonecleaner.ms321.ads

/**
 * Centralized Ad Configuration for Google AdMob.
 *
 * When USE_TEST_ADS = true:
 *   Uses Google's official sample Test Ad Unit IDs so you can freely test all ad formats
 *   without any policy violations, invalid traffic warnings, or account bans.
 *
 * When USE_TEST_ADS = false:
 *   Uses your real production AdMob Ad Unit IDs for publishing to Google Play Store.
 */
object AdConfig {
    // Change to false when uploading to Google Play Store
    const val USE_TEST_ADS = true

    // --- Official Google AdMob Sample Test IDs ---
    // Reference: https://developers.google.com/admob/android/test-ads
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_NATIVE_ADVANCE_ID = "ca-app-pub-3940256099942544/2247696110"

    // --- Real Production Ad Unit IDs ---
    const val REAL_APP_ID = "ca-app-pub-8393457688147659~6995482245"
    const val REAL_APP_OPEN_ID = "ca-app-pub-8393457688147659/4369318908"
    const val REAL_INTERSTITIAL_ID = "ca-app-pub-8393457688147659/5382269245"
    const val REAL_BANNER_ID = "ca-app-pub-8393457688147659/4069936000"
    const val REAL_NATIVE_ADVANCE_ID = "ca-app-pub-8393457688147659/5682400578"

    val APP_ID: String
        get() = if (USE_TEST_ADS) TEST_APP_ID else REAL_APP_ID

    val APP_OPEN_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_APP_OPEN_ID else REAL_APP_OPEN_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_ID else REAL_INTERSTITIAL_ID

    val BANNER_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_BANNER_ID else REAL_BANNER_ID

    val NATIVE_ADVANCE_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_ADVANCE_ID else REAL_NATIVE_ADVANCE_ID
}
