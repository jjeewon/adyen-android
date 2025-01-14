/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 20/2/2019.
 */
package com.adyen.checkout.components

import androidx.lifecycle.LifecycleOwner
import com.adyen.checkout.components.model.payments.request.PaymentMethodDetails

/**
 * A component that handles collecting user input data. It handles validating and formatting the data for the UI.
 * A valid [PaymentComponentState] contains [PaymentMethodDetails] to help compose the payments/ call on the backend.
 *
 *
 *
 * Should be used attached to a corresponding ComponentView to get data from.
 */
interface PaymentComponent<ComponentStateT : PaymentComponentState<*>> : Component {

    // TODO sessions: remove this and make it internal inside the components
    fun observe(lifecycleOwner: LifecycleOwner, callback: (PaymentComponentEvent<ComponentStateT>) -> Unit)

    // TODO sessions: remove this and make it internal inside the components
    fun removeObserver()
}
