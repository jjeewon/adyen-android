/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 18/11/2022.
 */

package com.adyen.checkout.googlepay

import com.adyen.checkout.components.base.ComponentParams
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.payments.Amount
import com.adyen.checkout.components.util.CheckoutCurrency
import com.adyen.checkout.components.util.isEmpty
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.core.exception.ComponentException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.googlepay.util.AllowedAuthMethods
import com.adyen.checkout.googlepay.util.AllowedCardNetworks
import com.google.android.gms.wallet.WalletConstants

internal class GooglePayComponentParamsMapper(
    private val overrideComponentParams: ComponentParams?,
) {

    fun mapToParams(
        googlePayConfiguration: GooglePayConfiguration,
        paymentMethod: PaymentMethod,
    ): GooglePayComponentParams {
        return googlePayConfiguration
            .mapToParamsInternal(paymentMethod)
            .override(overrideComponentParams)
    }

    private fun GooglePayConfiguration.mapToParamsInternal(
        paymentMethod: PaymentMethod,
    ): GooglePayComponentParams {
        return GooglePayComponentParams(
            shopperLocale = shopperLocale,
            environment = environment,
            clientKey = clientKey,
            isAnalyticsEnabled = isAnalyticsEnabled ?: true,
            isCreatedByDropIn = false,
            gatewayMerchantId = getPreferredGatewayMerchantId(paymentMethod),
            allowedAuthMethods = getAvailableAuthMethods(),
            allowedCardNetworks = getAvailableCardNetworks(paymentMethod),
            googlePayEnvironment = getGooglePayEnvironment(),
            amount = if (amount.isEmpty) DEFAULT_AMOUNT else amount,
            totalPriceStatus = totalPriceStatus ?: DEFAULT_TOTAL_PRICE_STATUS,
            countryCode = countryCode,
            merchantInfo = merchantInfo,
            isAllowPrepaidCards = isAllowPrepaidCards ?: false,
            isEmailRequired = isEmailRequired ?: false,
            isExistingPaymentMethodRequired = isExistingPaymentMethodRequired ?: false,
            isShippingAddressRequired = isShippingAddressRequired ?: false,
            shippingAddressParameters = shippingAddressParameters,
            isBillingAddressRequired = isBillingAddressRequired ?: false,
            billingAddressParameters = billingAddressParameters,
        )
    }

    /**
     * Returns the [GooglePayConfiguration.merchantAccount] if set, or falls back to the
     * paymentMethod.configuration.gatewayMerchantId field returned by the API.
     */
    private fun GooglePayConfiguration.getPreferredGatewayMerchantId(
        paymentMethod: PaymentMethod,
    ): String {
        return merchantAccount
            ?: paymentMethod.configuration?.gatewayMerchantId
            ?: throw ComponentException(
                "GooglePay merchantAccount not found. Update your API version or pass it manually inside your " +
                    "GooglePayConfiguration"
            )
    }

    private fun GooglePayConfiguration.getAvailableAuthMethods(): List<String> {
        return allowedAuthMethods
            ?: AllowedAuthMethods.allAllowedAuthMethods
    }

    private fun GooglePayConfiguration.getAvailableCardNetworks(
        paymentMethod: PaymentMethod
    ): List<String> {
        return allowedCardNetworks
            ?: getAvailableCardNetworksFromApi(paymentMethod)
            ?: AllowedCardNetworks.allAllowedCardNetworks
    }

    private fun getAvailableCardNetworksFromApi(paymentMethod: PaymentMethod): List<String>? {
        val brands = paymentMethod.brands ?: return null
        return brands.mapNotNull { brand ->
            val network = mapBrandToGooglePayNetwork(brand)
            if (network == null) Logger.e(TAG, "skipping brand $brand, as it is not an allowed card network.")
            return@mapNotNull network
        }
    }

    private fun mapBrandToGooglePayNetwork(brand: String): String? {
        return when {
            brand == "mc" -> AllowedCardNetworks.MASTERCARD
            AllowedCardNetworks.allAllowedCardNetworks.contains(brand.uppercase()) -> brand.uppercase()
            else -> null
        }
    }

    private fun GooglePayConfiguration.getGooglePayEnvironment(): Int {
        return when {
            googlePayEnvironment != null -> googlePayEnvironment
            environment == Environment.TEST -> WalletConstants.ENVIRONMENT_TEST
            else -> WalletConstants.ENVIRONMENT_PRODUCTION
        }
    }

    private fun GooglePayComponentParams.override(overrideComponentParams: ComponentParams?): GooglePayComponentParams {
        if (overrideComponentParams == null) return this
        val amount = if (overrideComponentParams.amount.isEmpty) DEFAULT_AMOUNT else overrideComponentParams.amount
        return copy(
            shopperLocale = overrideComponentParams.shopperLocale,
            environment = overrideComponentParams.environment,
            clientKey = overrideComponentParams.clientKey,
            isAnalyticsEnabled = overrideComponentParams.isAnalyticsEnabled,
            isCreatedByDropIn = overrideComponentParams.isCreatedByDropIn,
            amount = amount,
        )
    }

    companion object {
        private val TAG = LogUtil.getTag()
        private val DEFAULT_AMOUNT = Amount(currency = CheckoutCurrency.USD.name, value = 0)
        private const val DEFAULT_TOTAL_PRICE_STATUS = "FINAL"
    }
}
