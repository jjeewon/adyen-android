/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 29/11/2021.
 */

package com.adyen.checkout.dropin.ui.viewmodel

import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.payments.request.OrderRequest
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.dropin.ui.giftcard.GiftCardPaymentConfirmationData
import com.adyen.checkout.sessions.model.SessionModel

internal sealed class DropInActivityEvent {
    data class MakePartialPayment(val paymentComponentState: PaymentComponentState<*>) : DropInActivityEvent()
    object ShowPaymentMethods : DropInActivityEvent()
    class CancelOrder(val order: OrderRequest, val isDropInCancelledByUser: Boolean) : DropInActivityEvent()
    object CancelDropIn : DropInActivityEvent()
    class NavigateTo(val destination: DropInDestination) : DropInActivityEvent()
    data class SessionServiceConnected(
        val sessionModel: SessionModel,
        val clientKey: String,
        val environment: Environment,
        val isFlowTakenOver: Boolean,
    ) : DropInActivityEvent()
}

internal sealed class DropInDestination {
    object PreselectedStored : DropInDestination()
    object PaymentMethods : DropInDestination()
    class PaymentComponent(val paymentMethod: PaymentMethod) : DropInDestination()
    class GiftCardPaymentConfirmation(val data: GiftCardPaymentConfirmationData) : DropInDestination()
}
