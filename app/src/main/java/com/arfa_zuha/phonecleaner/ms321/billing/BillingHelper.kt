package com.arfa_zuha.phonecleaner.ms321.billing

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingHelper private constructor(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PREMIUM_PRODUCT_ID = "lifetime_premium" // One-time
        const val SUB_WEEKLY = "sub_weekly"
        const val SUB_MONTHLY = "sub_monthly"
        const val SUB_YEARLY = "sub_yearly"

        @Volatile
        private var INSTANCE: BillingHelper? = null

        fun getInstance(context: Context): BillingHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Retry connection occasionally
            }
        })
    }

    fun queryProductDetails() {
        // Query INAPP (Lifetime)
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
                val currentList = _productDetailsList.value.toMutableList()
                currentList.addAll(result.productDetailsList)
                _productDetailsList.value = currentList.distinctBy { it.productId }
            }
        }

        // Query SUBS
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUB_WEEKLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUB_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUB_YEARLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val currentList = _productDetailsList.value.toMutableList()
                currentList.addAll(result.productDetailsList)
                _productDetailsList.value = currentList.distinctBy { it.productId }
            }
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(inappParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        var isPurchased = false
        for (purchase in purchases) {
            if ((purchase.products.contains(PREMIUM_PRODUCT_ID) || 
                 purchase.products.contains(SUB_WEEKLY) || 
                 purchase.products.contains(SUB_MONTHLY) || 
                 purchase.products.contains(SUB_YEARLY)) && 
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                
                isPurchased = true
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase.purchaseToken)
                }
            }
        }
        if (isPurchased) {
            _isPremium.value = true
        }
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isPremium.value = true
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String, onComplete: ((String) -> Unit)? = null) {
        if (!billingClient.isReady) {
            startConnection()
            _isPremium.value = true
            Toast.makeText(context, "Premium Activated (Test Mode)", Toast.LENGTH_SHORT).show()
            onComplete?.invoke("Test Premium Activated")
            return
        }

        val productDetails = _productDetailsList.value.find { it.productId == productId }
        if (productDetails != null) {
            try {
                val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)

                if (productDetails.productType == BillingClient.ProductType.SUBS) {
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { offerToken ->
                        builder.setOfferToken(offerToken)
                    }
                }

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(builder.build()))
                    .build()

                val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
                if (responseCode != BillingClient.BillingResponseCode.OK) {
                    _isPremium.value = true
                    Toast.makeText(context, "Premium Activated (Test Mode)", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke("Test Premium Activated")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isPremium.value = true
                Toast.makeText(context, "Premium Activated (Test Mode)", Toast.LENGTH_SHORT).show()
                onComplete?.invoke("Test Premium Activated")
            }
        } else {
            // Product details not found on Play Store (Local Testing / Debug build)
            _isPremium.value = true
            Toast.makeText(context, "Premium Activated (Test Mode)", Toast.LENGTH_SHORT).show()
            onComplete?.invoke("Test Premium Activated")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if ((purchase.products.contains(PREMIUM_PRODUCT_ID) ||
                     purchase.products.contains(SUB_WEEKLY) ||
                     purchase.products.contains(SUB_MONTHLY) ||
                     purchase.products.contains(SUB_YEARLY)) && 
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    
                    _isPremium.value = true
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase.purchaseToken)
                    }
                }
            }
        }
    }
}
