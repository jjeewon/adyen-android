/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 19/7/2022.
 */

package com.adyen.checkout.googlepay

import android.app.Activity
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.analytics.AnalyticsRepository
import com.adyen.checkout.components.channel.bufferedChannel
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.repository.PaymentObserverRepository
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.exception.ComponentException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.googlepay.util.GooglePayUtils
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class DefaultGooglePayDelegate(
    private val observerRepository: PaymentObserverRepository,
    private val paymentMethod: PaymentMethod,
    override val componentParams: GooglePayComponentParams,
    private val analyticsRepository: AnalyticsRepository,
) : GooglePayDelegate {

    private val _componentStateFlow = MutableStateFlow(createComponentState())
    override val componentStateFlow: Flow<GooglePayComponentState> = _componentStateFlow

    private val exceptionChannel: Channel<CheckoutException> = bufferedChannel()
    override val exceptionFlow: Flow<CheckoutException> = exceptionChannel.receiveAsFlow()

    private val submitChannel: Channel<GooglePayComponentState> = bufferedChannel()
    override val submitFlow: Flow<GooglePayComponentState> = submitChannel.receiveAsFlow()

    override fun initialize(coroutineScope: CoroutineScope) {
        sendAnalyticsEvent(coroutineScope)

        componentStateFlow.onEach {
            onState(it)
        }.launchIn(coroutineScope)
    }

    private fun sendAnalyticsEvent(coroutineScope: CoroutineScope) {
        Logger.v(TAG, "sendAnalyticsEvent")
        coroutineScope.launch {
            analyticsRepository.sendAnalyticsEvent()
        }
    }

    private fun onState(state: GooglePayComponentState) {
        if (state.isValid) {
            submitChannel.trySend(state)
        }
    }

    override fun observe(
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        callback: (PaymentComponentEvent<GooglePayComponentState>) -> Unit
    ) {
        observerRepository.addObservers(
            stateFlow = componentStateFlow,
            exceptionFlow = exceptionFlow,
            submitFlow = submitFlow,
            lifecycleOwner = lifecycleOwner,
            coroutineScope = coroutineScope,
            callback = callback
        )
    }

    override fun removeObserver() {
        observerRepository.removeObservers()
    }

    @VisibleForTesting
    internal fun updateComponentState(paymentData: PaymentData?) {
        Logger.v(TAG, "updateComponentState")
        val componentState = createComponentState(paymentData)
        _componentStateFlow.tryEmit(componentState)
    }

    private fun createComponentState(paymentData: PaymentData? = null): GooglePayComponentState {
        val isValid = paymentData?.let {
            GooglePayUtils.findToken(it).isNotEmpty()
        } ?: false

        val paymentMethod = GooglePayUtils.createGooglePayPaymentMethod(paymentData, paymentMethod.type)
        val paymentComponentData = PaymentComponentData(paymentMethod = paymentMethod)

        return GooglePayComponentState(
            paymentComponentData = paymentComponentData,
            isInputValid = isValid,
            isReady = true,
            paymentData = paymentData
        )
    }

    override fun startGooglePayScreen(activity: Activity, requestCode: Int) {
        Logger.d(TAG, "startGooglePayScreen")
        val paymentsClient = Wallet.getPaymentsClient(activity, GooglePayUtils.createWalletOptions(componentParams))
        val paymentDataRequest = GooglePayUtils.createPaymentDataRequest(componentParams)
        // TODO this forces us to use the deprecated onActivityResult. Look into alternatives when/if Google provides
        //  any later.
        AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(paymentDataRequest), activity, requestCode)
    }

    override fun handleActivityResult(resultCode: Int, data: Intent?) {
        Logger.d(TAG, "handleActivityResult")
        when (resultCode) {
            Activity.RESULT_OK -> {
                if (data == null) {
                    exceptionChannel.trySend(ComponentException("Result data is null"))
                    return
                }
                val paymentData = PaymentData.getFromIntent(data)
                updateComponentState(paymentData)
            }
            Activity.RESULT_CANCELED -> {
                exceptionChannel.trySend(ComponentException("Payment canceled."))
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                val statusMessage: String = status?.let { ": ${it.statusMessage}" }.orEmpty()
                exceptionChannel.trySend(ComponentException("GooglePay returned an error$statusMessage"))
            }
            else -> Unit
        }
    }

    override fun getPaymentMethodType(): String {
        return paymentMethod.type ?: PaymentMethodTypes.UNKNOWN
    }

    override fun onCleared() {
        removeObserver()
    }

    companion object {
        private val TAG = LogUtil.getTag()
    }
}
