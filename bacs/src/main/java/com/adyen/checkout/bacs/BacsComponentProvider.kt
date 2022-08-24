/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 12/4/2022.
 */

package com.adyen.checkout.bacs

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.adyen.checkout.components.PaymentComponentProvider
import com.adyen.checkout.components.base.lifecycle.viewModelFactory
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod

class BacsComponentProvider : PaymentComponentProvider<BacsDirectDebitComponent, BacsDirectDebitConfiguration> {
    override fun get(
        savedStateRegistryOwner: SavedStateRegistryOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        paymentMethod: PaymentMethod,
        configuration: BacsDirectDebitConfiguration,
        defaultArgs: Bundle?
    ): BacsDirectDebitComponent {
        val genericFactory: ViewModelProvider.Factory =
            viewModelFactory(savedStateRegistryOwner, defaultArgs) { savedStateHandle ->
                BacsDirectDebitComponent(
                    savedStateHandle,
                    DefaultBacsDirectDebitDelegate(paymentMethod),
                    configuration
                )
            }
        return ViewModelProvider(viewModelStoreOwner, genericFactory).get(BacsDirectDebitComponent::class.java)
    }
}
