/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 22/8/2019.
 */
package com.adyen.checkout.sepa

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.adyen.checkout.components.base.BaseConfigurationBuilder
import com.adyen.checkout.components.base.Configuration
import com.adyen.checkout.core.api.Environment
import java.util.Locale

class SepaConfiguration : Configuration {
    internal constructor(
        shopperLocale: Locale,
        environment: Environment,
        clientKey: String,
    ) : super(shopperLocale, environment, clientKey)
    internal constructor(parcel: Parcel) : super(parcel)

    /**
     * Builder to create a [SepaConfiguration].
     */
    class Builder : BaseConfigurationBuilder<SepaConfiguration> {
        /**
         * Constructor for Builder with default values.
         *
         * @param context   A context
         * @param clientKey Your Client Key used for network calls from the SDK to Adyen.
         */
        constructor(context: Context, clientKey: String) : super(context, clientKey)

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

        /**
         * Constructor that copies an existing configuration.
         *
         * @param configuration A configuration to initialize the builder.
         */
        constructor(configuration: SepaConfiguration) : super(configuration)

        override fun setShopperLocale(builderShopperLocale: Locale): Builder {
            return super.setShopperLocale(builderShopperLocale) as Builder
        }

        override fun setEnvironment(builderEnvironment: Environment): Builder {
            return super.setEnvironment(builderEnvironment) as Builder
        }

        override fun buildInternal(): SepaConfiguration {
            return SepaConfiguration(
                shopperLocale = builderShopperLocale,
                environment = builderEnvironment,
                clientKey = builderClientKey
            )
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SepaConfiguration> = object : Parcelable.Creator<SepaConfiguration> {
            override fun createFromParcel(parcel: Parcel): SepaConfiguration {
                return SepaConfiguration(parcel)
            }

            override fun newArray(size: Int): Array<SepaConfiguration?> {
                return arrayOfNulls(size)
            }
        }
    }
}
