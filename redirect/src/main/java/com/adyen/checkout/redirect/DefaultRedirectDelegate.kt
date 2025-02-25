/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 18/8/2022.
 */

package com.adyen.checkout.redirect

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.ActionComponentEvent
import com.adyen.checkout.components.base.GenericComponentParams
import com.adyen.checkout.components.channel.bufferedChannel
import com.adyen.checkout.components.handler.RedirectHandler
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.components.model.payments.response.RedirectAction
import com.adyen.checkout.components.repository.ActionObserverRepository
import com.adyen.checkout.components.repository.PaymentDataRepository
import com.adyen.checkout.components.ui.view.ComponentViewType
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.exception.ComponentException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.json.JSONObject

private val TAG = LogUtil.getTag()

internal class DefaultRedirectDelegate(
    private val observerRepository: ActionObserverRepository,
    override val componentParams: GenericComponentParams,
    private val redirectHandler: RedirectHandler,
    private val paymentDataRepository: PaymentDataRepository,
) : RedirectDelegate {

    private val detailsChannel: Channel<ActionComponentData> = bufferedChannel()
    override val detailsFlow: Flow<ActionComponentData> = detailsChannel.receiveAsFlow()

    private val exceptionChannel: Channel<CheckoutException> = bufferedChannel()
    override val exceptionFlow: Flow<CheckoutException> = exceptionChannel.receiveAsFlow()

    override val viewFlow: Flow<ComponentViewType?> = MutableStateFlow(RedirectComponentViewType)

    override fun initialize(coroutineScope: CoroutineScope) {
        // no ops
    }

    override fun observe(
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        callback: (ActionComponentEvent) -> Unit
    ) {
        observerRepository.addObservers(
            detailsFlow = detailsFlow,
            exceptionFlow = exceptionFlow,
            lifecycleOwner = lifecycleOwner,
            coroutineScope = coroutineScope,
            callback = callback
        )
    }

    override fun removeObserver() {
        observerRepository.removeObservers()
    }

    override fun handleAction(action: Action, activity: Activity) {
        if (action !is RedirectAction) {
            exceptionChannel.trySend(ComponentException("Unsupported action"))
            return
        }

        paymentDataRepository.paymentData = action.paymentData
        makeRedirect(activity, action.url)
    }

    private fun makeRedirect(activity: Activity, url: String?) {
        try {
            Logger.d(TAG, "makeRedirect - $url")
            // TODO look into emitting a value to tell observers that a redirect was launched so they can track its
            //  status when the app resumes. Currently we have no way of doing that but we can create something like
            //  PaymentComponentState for actions.
            redirectHandler.launchUriRedirect(activity, url)
        } catch (ex: CheckoutException) {
            exceptionChannel.trySend(ex)
        }
    }

    override fun handleIntent(intent: Intent) {
        try {
            val details = redirectHandler.parseRedirectResult(intent.data)
            detailsChannel.trySend(createActionComponentData(details))
        } catch (ex: CheckoutException) {
            exceptionChannel.trySend(ex)
        }
    }

    private fun createActionComponentData(details: JSONObject): ActionComponentData {
        return ActionComponentData(
            details = details,
            paymentData = paymentDataRepository.paymentData,
        )
    }

    override fun onError(e: CheckoutException) {
        exceptionChannel.trySend(e)
    }

    override fun onCleared() {
        removeObserver()
    }
}
