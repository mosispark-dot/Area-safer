package com.arfa_zuha.phonecleaner.ms321.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingHelper private constructor(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        /**
         * Product ID of the ONE subscription created in Play Console
         * (From Google Play Console: "weekly_4").
         */
        const val SUBSCRIPTION_PRODUCT_ID = "weekly_4"

        // Base plan IDs inside that subscription (must match Play Console exactly).
        // The same constants are used as "plan keys" by the paywall UI.
        const val SUB_WEEKLY = "weekly4"
        const val SUB_MONTHLY = "montly"
        const val SUB_YEARLY = "yearly"

        // One-time lifetime product
        const val PREMIUM_PRODUCT_ID = "lifetime_premium"

        private val OWNED_IDS = listOf(SUBSCRIPTION_PRODUCT_ID, SUB_WEEKLY, SUB_MONTHLY, SUB_YEARLY, PREMIUM_PRODUCT_ID)

        private const val PREFS = "billing_prefs"
        private const val KEY_PREMIUM = "is_premium"

        @Volatile
        private var INSTANCE: BillingHelper? = null

        fun getInstance(context: Context): BillingHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    // Cached value so ads/premium state is correct immediately on app start (verified again by queryPurchases()).
    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()

    @Volatile
    private var diagnostics: String = "not queried yet"

    init {
        startConnection()
    }

    private fun setPremium(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
        _isPremium.value = value
    }

    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    private fun startConnection(onReady: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            onReady?.invoke()
            return
        }
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTING) return

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()
                    onReady?.invoke()
                } else {
                    diagnostics = "Billing setup failed code=${billingResult.responseCode} msg=${billingResult.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                mainHandler.postDelayed({ startConnection() }, 3000)
            }
        })
    }

    fun queryProductDetails() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val subsCandidates = mutableListOf<String>()
        if (SUBSCRIPTION_PRODUCT_ID.isNotBlank() && !SUBSCRIPTION_PRODUCT_ID.startsWith("REPLACE_")) {
            subsCandidates.add(SUBSCRIPTION_PRODUCT_ID)
        }
        subsCandidates.addAll(listOf(SUB_WEEKLY, SUB_MONTHLY, SUB_YEARLY, "sub_weekly", "sub_monthly", "sub_yearly"))
        val distinctSubs = subsCandidates.distinct()

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                distinctSubs.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                mergeProducts(result.productDetailsList)
            }
            diagnostics = "SUBS code=${billingResult.responseCode} " +
                    "found=${result.productDetailsList.map { it.productId }} " +
                    "notFound=${result.unfetchedProductList.map { it.productId }} " +
                    "msg=${billingResult.debugMessage}"
            android.util.Log.w("BillingHelper", diagnostics)
        }

        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(inappParams) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                mergeProducts(result.productDetailsList)
            }
            android.util.Log.w(
                "BillingHelper",
                "INAPP code=${billingResult.responseCode} found=${result.productDetailsList.map { it.productId }} " +
                        "notFound=${result.unfetchedProductList.map { it.productId }} msg=${billingResult.debugMessage}"
            )
        }
    }

    @Synchronized
    private fun mergeProducts(newItems: List<ProductDetails>) {
        _productDetailsList.value = (_productDetailsList.value + newItems).distinctBy { it.productId }
    }

    /** Re-checks what the user owns on Google Play. Call on app start and on resume. */
    fun queryPurchases() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        var inappOwned: Boolean? = null
        var subsOwned: Boolean? = null

        fun publish() {
            val inapp = inappOwned ?: return
            val subs = subsOwned ?: return
            // Both queries answered OK: this is the real truth (also revokes expired/cancelled subs)
            setPremium(inapp || subs)
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                inappOwned = handlePurchases(purchases)
                publish()
            }
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subsOwned = handlePurchases(purchases)
                publish()
            }
        }
    }

    /** Explicitly restores past purchases when user taps 'Restore Purchases' in UI. */
    fun restorePurchases(onResult: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            startConnection { restorePurchases(onResult) }
            return
        }
        var inappOwned: Boolean? = null
        var subsOwned: Boolean? = null

        fun check() {
            val inapp = inappOwned ?: return
            val subs = subsOwned ?: return
            val owned = inapp || subs
            setPremium(owned)
            mainHandler.post { onResult(owned) }
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchases ->
            inappOwned = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(purchases) else false
            check()
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchases ->
            subsOwned = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(purchases) else false
            check()
        }
    }

    /** Returns true if the list contains a valid purchased premium product. Acknowledges if needed. */
    private fun handlePurchases(purchases: List<Purchase>): Boolean {
        var owned = false
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                owned = true
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase.purchaseToken)
                }
            }
        }
        return owned
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { /* queryPurchases will retry next time if it failed */ }
    }

    private fun subscriptionDetails(): ProductDetails? =
        _productDetailsList.value.find { it.productId == SUBSCRIPTION_PRODUCT_ID }

    /**
     * Picks the offer for the given base plan: one with a free trial if it exists,
     * otherwise the plain base plan offer (offerId == null), otherwise the first match.
     */
    private fun selectOffer(details: ProductDetails, basePlanId: String): ProductDetails.SubscriptionOfferDetails? {
        val offers = details.subscriptionOfferDetails
            ?.filter { it.basePlanId == basePlanId }
            .orEmpty()
        return offers.firstOrNull { offer -> offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
            ?: offers.firstOrNull { it.offerId == null }
            ?: offers.firstOrNull()
    }

    /** Formatted real price from Play (e.g. "$4.00") for a base plan or the lifetime product, or null if not loaded. */
    fun getFormattedPrice(planId: String): String? {
        if (planId == PREMIUM_PRODUCT_ID) {
            return _productDetailsList.value.find { it.productId == PREMIUM_PRODUCT_ID }
                ?.oneTimePurchaseOfferDetails?.formattedPrice
        }

        // 1. Direct subscription product (e.g. subscription productId is "weekly4")
        val directProduct = _productDetailsList.value.find { it.productId == planId }
        if (directProduct != null) {
            val offers = directProduct.subscriptionOfferDetails.orEmpty()
            val baseOffer = offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
            val price = baseOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
            if (price != null) return price
        }

        // 2. Base plan inside a parent subscription product
        for (prod in _productDetailsList.value) {
            val offers = prod.subscriptionOfferDetails?.filter { it.basePlanId == planId }.orEmpty()
            if (offers.isNotEmpty()) {
                val baseOffer = offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
                val price = baseOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                if (price != null) return price
            }
        }
        return null
    }

    fun launchPurchaseFlow(activity: Activity, planId: String, onComplete: ((String) -> Unit)? = null) {
        if (!billingClient.isReady) {
            toast("Connecting to Google Play... please try again in a moment.")
            startConnection()
            return
        }

        // One-time Lifetime purchase
        if (planId == PREMIUM_PRODUCT_ID) {
            val lifetime = _productDetailsList.value.find { it.productId == PREMIUM_PRODUCT_ID }
            if (lifetime == null) {
                toast("Lifetime product '$PREMIUM_PRODUCT_ID' not found. Check Play Console > In-app products.")
                queryProductDetails()
                return
            }
            val lifetimeParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(lifetime)
                .build()
            val lifetimeResult = billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(lifetimeParams)).build()
            )
            if (lifetimeResult.responseCode != BillingClient.BillingResponseCode.OK) {
                toast("Unable to start purchase: ${lifetimeResult.debugMessage}")
            }
            return
        }

        // Check if there is a direct product matching planId (Case B: separate subscription per plan)
        var targetProduct = _productDetailsList.value.find { it.productId == planId }
        var targetOffer: ProductDetails.SubscriptionOfferDetails? = null

        if (targetProduct != null) {
            val offers = targetProduct.subscriptionOfferDetails.orEmpty()
            targetOffer = offers.firstOrNull { offer -> offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
                ?: offers.firstOrNull { it.offerId == null }
                ?: offers.firstOrNull()
        } else {
            // Check if planId is a basePlanId inside any parent subscription (Case A)
            for (prod in _productDetailsList.value) {
                val match = selectOffer(prod, planId)
                if (match != null) {
                    targetProduct = prod
                    targetOffer = match
                    break
                }
            }
        }

        if (targetProduct == null || targetOffer == null) {
            val available = _productDetailsList.value.map { it.productId }
            toast("Plan '$planId' not available.\nPlay Products: $available\n$diagnostics")
            queryProductDetails()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(targetProduct)
            .setOfferToken(targetOffer.offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            toast("Unable to start purchase: ${result.debugMessage}")
        }
        // On success, premium is granted in onPurchasesUpdated()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null && handlePurchases(purchases)) {
                    setPremium(true)
                    toast("Premium activated. Thank you!")
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases()
            BillingClient.BillingResponseCode.USER_CANCELED -> { /* user closed the dialog */ }
            else -> toast("Purchase failed: ${billingResult.debugMessage}")
        }
    }
}
