/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 12/6/2019.
 */
package com.adyen.checkout.dotpay

import android.content.Context
import com.adyen.checkout.action.GenericActionConfiguration
import com.adyen.checkout.components.model.payments.Amount
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.issuerlist.IssuerListConfiguration
import com.adyen.checkout.issuerlist.IssuerListViewType
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
@Suppress("LongParameterList")
class DotpayConfiguration private constructor(
    override val shopperLocale: Locale,
    override val environment: Environment,
    override val clientKey: String,
    override val isAnalyticsEnabled: Boolean?,
    override val amount: Amount,
    override val viewType: IssuerListViewType?,
    override val isSubmitButtonVisible: Boolean?,
    override val hideIssuerLogos: Boolean?,
    internal val genericActionConfiguration: GenericActionConfiguration,
) : IssuerListConfiguration() {

    /**
     * Builder to create a [DotpayConfiguration].
     */
    class Builder : IssuerListBuilder<DotpayConfiguration, Builder> {

        /**
         * Constructor for Builder with default values.
         *
         * @param context   A context
         * @param environment   The [Environment] to be used for network calls to Adyen.
         * @param clientKey Your Client Key used for network calls from the SDK to Adyen.
         */
        constructor(context: Context, environment: Environment, clientKey: String) : super(
            context,
            environment,
            clientKey
        )

        /**
         * Builder with required parameters.
         *
         * @param shopperLocale The Locale of the shopper.
         * @param environment   The [Environment] to be used for network calls to Adyen.
         * @param clientKey Your Client Key used for network calls from the SDK to Adyen.
         */
        constructor(
            shopperLocale: Locale,
            environment: Environment,
            clientKey: String
        ) : super(shopperLocale, environment, clientKey)

        override fun buildInternal(): DotpayConfiguration {
            return DotpayConfiguration(
                shopperLocale = shopperLocale,
                environment = environment,
                clientKey = clientKey,
                isAnalyticsEnabled = isAnalyticsEnabled,
                amount = amount,
                viewType = viewType,
                hideIssuerLogos = hideIssuerLogos,
                isSubmitButtonVisible = isSubmitButtonVisible,
                genericActionConfiguration = genericActionConfigurationBuilder.build(),
            )
        }
    }
}
