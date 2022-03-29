/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 12/6/2019.
 */
package com.adyen.checkout.entercash

import androidx.lifecycle.SavedStateHandle
import com.adyen.checkout.components.PaymentComponentProvider
import com.adyen.checkout.components.base.GenericPaymentComponentProvider
import com.adyen.checkout.components.base.GenericPaymentMethodDelegate
import com.adyen.checkout.components.model.payments.request.EntercashPaymentMethod
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.issuerlist.IssuerListComponent

/**
 * PaymentComponent to handle iDeal payments.
 */
class EntercashComponent(
    savedStateHandle: SavedStateHandle,
    paymentMethodDelegate: GenericPaymentMethodDelegate,
    configuration: EntercashConfiguration
) : IssuerListComponent<EntercashPaymentMethod>(savedStateHandle, paymentMethodDelegate, configuration) {

    override fun instantiateTypedPaymentMethod(): EntercashPaymentMethod {
        return EntercashPaymentMethod()
    }

    override val supportedPaymentMethodTypes: Array<String> = arrayOf(PaymentMethodTypes.ENTERCASH)

    companion object {
        @JvmField
        val PROVIDER: PaymentComponentProvider<EntercashComponent, EntercashConfiguration> = GenericPaymentComponentProvider(
            EntercashComponent::class.java
        )
    }
}
