/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 3/1/2023.
 */

package com.adyen.checkout.sessions

import android.util.Log
import androidx.annotation.RestrictTo
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.model.payments.request.OrderRequest
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.sessions.interactor.SessionCallResult
import com.adyen.checkout.sessions.interactor.SessionInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
class SessionHandler(
    private val sessionInteractor: SessionInteractor,
    private val coroutineScope: CoroutineScope,
    private val sessionSavedStateHandleContainer: SessionSavedStateHandleContainer
) {

    init {
        coroutineScope.launch {
            sessionInteractor.sessionFlow
                .mapNotNull { it.sessionData }
                .collect { updateSessionData(it) }
        }
    }

    fun onPaymentComponentEvent(event: PaymentComponentEvent<*>) {
        Logger.e(TAG, "Event received $event") // TODO sessions: remove
        when (event) {
            is PaymentComponentEvent.ActionDetails -> onDetailsCallRequested(event.data)
            is PaymentComponentEvent.Error -> onError(event.error)
            is PaymentComponentEvent.StateChanged -> onState(event.state)
            is PaymentComponentEvent.Submit -> onPaymentsCallRequested(event.state)
        }
    }

    private fun updateSessionData(sessionData: String) {
        Logger.v(TAG, "Updating session data - $sessionData")
        sessionSavedStateHandleContainer.updateSessionData(sessionData)
    }

    private fun onPaymentsCallRequested(
        paymentComponentState: PaymentComponentState<*>,
    ) {
        val paymentComponentJson = PaymentComponentData.SERIALIZER.serialize(paymentComponentState.data)

        coroutineScope.launch {
            val result = sessionInteractor.onPaymentsCallRequested(
                paymentComponentState,
                { makePaymentsCallMerchant(it, paymentComponentJson) },
                ::makePaymentsCallMerchant.name,
            )

            when (result) {
                is SessionCallResult.Payments.Action -> {
                    // TODO sessions: delegate action handling to merchant
                }
                is SessionCallResult.Payments.Error -> onError(result.reason)
                is SessionCallResult.Payments.Finished -> onFinished(result.resultCode)
                is SessionCallResult.Payments.NotFullyPaidOrder -> {
                    // TODO sessions: handle with gift card
                }
                is SessionCallResult.Payments.TakenOver -> {
                    setFlowTakenOver()
                }
            }
        }
    }

    private fun onDetailsCallRequested(
        actionComponentData: ActionComponentData
    ) {
        val actionComponentJson = ActionComponentData.SERIALIZER.serialize(actionComponentData)

        coroutineScope.launch {
            val result = sessionInteractor.onDetailsCallRequested(
                actionComponentData,
                { makeDetailsCallMerchant(it, actionComponentJson) },
                ::makeDetailsCallMerchant.name
            )

            when (result) {
                is SessionCallResult.Details.Action -> {
                    // TODO sessions: delegate action handling to merchant
                }
                is SessionCallResult.Details.Error -> onError(result.reason)
                is SessionCallResult.Details.Finished -> onFinished(result.resultCode)
                SessionCallResult.Details.TakenOver -> {
                    setFlowTakenOver()
                }
            }
        }
    }

    private fun checkBalance(paymentMethodData: PaymentMethodDetails) {
        coroutineScope.launch {
            val result = sessionInteractor.checkBalance(
                paymentMethodData,
                ::makeCheckBalanceCallMerchant,
                ::makeCheckBalanceCallMerchant.name,
            )
            when (result) {
                is SessionCallResult.Balance.Error -> onError(result.reason)
                is SessionCallResult.Balance.Successful -> {
                    // TODO sessions: handle with gift card
                }
                SessionCallResult.Balance.TakenOver -> {
                    setFlowTakenOver()
                }
            }
        }
    }

    private fun createOrder() {
        coroutineScope.launch {
            val result = sessionInteractor.createOrder(
                ::makeCreateOrderMerchant,
                ::makeCreateOrderMerchant.name
            )

            when (result) {
                is SessionCallResult.CreateOrder.Error -> onError(result.reason)
                is SessionCallResult.CreateOrder.Successful -> {
                    // TODO sessions: handle with gift card
                }
                SessionCallResult.CreateOrder.TakenOver -> {
                    setFlowTakenOver()
                }
            }
        }
    }

    private fun cancelOrder(order: OrderRequest, shouldUpdatePaymentMethods: Boolean) {
        coroutineScope.launch {
            val result = sessionInteractor.cancelOrder(
                order,
                { makeCancelOrderCallMerchant(it, shouldUpdatePaymentMethods) },
                ::makeCancelOrderCallMerchant.name
            )

            when (result) {
                is SessionCallResult.CancelOrder.Error -> onError(result.reason)
                SessionCallResult.CancelOrder.Successful -> {
                    // TODO sessions: handle with gift card
                }
                SessionCallResult.CancelOrder.TakenOver -> {
                    setFlowTakenOver()
                }
            }
        }
    }

    private fun onState(state: PaymentComponentState<*>) {
        // TODO sessions: provide merchants with state
    }

    private fun onError(error: ComponentError) {
        // TODO sessions: provide merchants with error
    }

    private fun onError(reason: String?) {
        // TODO sessions: provide merchants with error
    }

    private fun onFinished(resultCode: String) {
        Log.d(TAG, "Finished: $resultCode")
    }

    private fun setFlowTakenOver() {
        if (sessionSavedStateHandleContainer.isFlowTakenOver == true) return
        sessionSavedStateHandleContainer.isFlowTakenOver = true
        Logger.i(TAG, "Flow was taken over.")
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun makePaymentsCallMerchant(
        paymentComponentState: PaymentComponentState<*>,
        paymentComponentJson: JSONObject
    ): Boolean = false

    @Suppress("FunctionOnlyReturningConstant")
    private fun makeDetailsCallMerchant(
        actionComponentData: ActionComponentData,
        actionComponentJson: JSONObject
    ): Boolean = false

    @Suppress("FunctionOnlyReturningConstant")
    private fun makeCheckBalanceCallMerchant(paymentMethodData: PaymentMethodDetails): Boolean = false

    @Suppress("FunctionOnlyReturningConstant")
    private fun makeCreateOrderMerchant(): Boolean = false

    @Suppress("FunctionOnlyReturningConstant")
    private fun makeCancelOrderCallMerchant(
        order: OrderRequest,
        shouldUpdatePaymentMethods: Boolean
    ): Boolean = false

    companion object {
        private val TAG = LogUtil.getTag()
    }
}
