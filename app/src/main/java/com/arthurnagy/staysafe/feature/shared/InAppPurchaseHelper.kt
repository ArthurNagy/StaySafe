package com.arthurnagy.staysafe.feature.shared

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

typealias OnConnected = (billingResult: BillingResult) -> Unit
typealias OnDisconnected = () -> Unit

object InAppPurchaseHelper {
    private var purchaseChecked: Boolean = false

    fun startPurchaseFlow(billingClient: BillingClient, onConnected: OnConnected? = null, onDisconnected: OnDisconnected? = null) {
        billingClient.startConnection(SimpleBillingClientListener(onConnected, onDisconnected))
    }

    fun checkPurchases(billingClient: BillingClient, onConnected: OnConnected? = null, onDisconnected: OnDisconnected? = null) {
        if (!purchaseChecked) {
            billingClient.startConnection(
                SimpleBillingClientListener(
                    onConnected = { billingResult ->
                        purchaseChecked = true
                        onConnected?.invoke(billingResult)
                    },
                    onDisconnected = onDisconnected
                )
            )
        }
    }

    suspend fun consumeAlreadyPurchased(billingClient: BillingClient) {
        val purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build()
        )
        if (purchasesResult.billingResult.isOk) {
            val unConsumedPurchases = purchasesResult.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            unConsumedPurchases.forEach { purchase ->
                consumePurchase(billingClient, purchase)
            }
        }
    }

    suspend fun launchBillingFlow(billingClient: BillingClient, activity: Activity): BillingResult? {
        return querySkuDetails(billingClient)?.let { productDetails ->
            billingClient.launchBillingFlow(
                activity, BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()
            )
        }
    }

    suspend fun consumePurchase(billingClient: BillingClient, purchase: Purchase): PurchaseResult {
        Timber.d("consumePurchase: purchase: $purchase")
        val result = if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val consumeResult = withContext(Dispatchers.IO) {
                    billingClient.consume(
                        ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            // .setDeveloperPayload(purchase.developerPayload)
                            .build()
                    )
                }
                if (consumeResult.billingResult.isOk) {
                    PurchaseResult.Success
                } else {
                    PurchaseResult.Error
                }
            } else PurchaseResult.Ignored
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            return PurchaseResult.Pending
        } else PurchaseResult.Ignored
        Timber.d("consumePurchase: result: $result")
        return result
    }

    sealed interface PurchaseResult {
        data object Success : PurchaseResult
        data object Ignored : PurchaseResult
        data object Pending : PurchaseResult
        data object Error : PurchaseResult
    }

    private suspend fun querySkuDetails(billingClient: BillingClient): ProductDetails? {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("staysafe.buy.me.a.coffee")
                .setProductType(ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        val productDetailsResult = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params)
        }
        Timber.d("querySkuDetails: skuDetailsResult: $productDetailsResult, code:${productDetailsResult.billingResult.responseCode}")
        return if (productDetailsResult.billingResult.isOk) {
            productDetailsResult.productDetailsList?.firstOrNull()
        } else null
    }

    private suspend fun BillingClient.consume(consumeParams: ConsumeParams) = suspendCancellableCoroutine {
        consumeAsync(consumeParams) { billingResult: BillingResult, s: String ->
            it.resume(ConsumeResult(billingResult, s))
        }
    }

    val BillingResult.isOk: Boolean get() = responseCode == BillingClient.BillingResponseCode.OK

    private class SimpleBillingClientListener(
        private val onConnected: OnConnected? = null,
        private val onDisconnected: OnDisconnected? = null
    ) : BillingClientStateListener {

        override fun onBillingServiceDisconnected() {
            onDisconnected?.invoke()
        }

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.isOk) {
                onConnected?.invoke(billingResult)
            } else {
                onDisconnected?.invoke()
            }
        }
    }

    abstract class SimplePurchaseListener : PurchasesUpdatedListener {

        open fun onPurchase(purchases: MutableList<Purchase>) = Unit
        open fun onUserCanceled() = Unit
        open fun onError(purchaseResult: BillingResult) = Unit

        override fun onPurchasesUpdated(purchaseResult: BillingResult, purchases: MutableList<Purchase>?) {
            if (purchaseResult.isOk && !purchases.isNullOrEmpty()) {
                onPurchase(purchases)
            } else if (purchaseResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                onUserCanceled()
            } else {
                onError(purchaseResult)
            }
        }
    }
}