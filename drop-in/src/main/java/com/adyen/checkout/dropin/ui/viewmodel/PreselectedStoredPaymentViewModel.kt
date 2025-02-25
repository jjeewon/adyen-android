/*
 * Copyright (c) 2020 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 2/12/2020.
 */

package com.adyen.checkout.dropin.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.channel.bufferedChannel
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.components.model.payments.Amount
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.R
import com.adyen.checkout.dropin.ui.paymentmethods.StoredPaymentMethodModel
import com.adyen.checkout.dropin.ui.stored.mapStoredModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale

internal class PreselectedStoredPaymentViewModel(
    private val storedPaymentMethod: StoredPaymentMethod,
    private val amount: Amount,
    private val dropInConfiguration: DropInConfiguration,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(getInitialUIState())
    val uiStateFlow: Flow<PreselectedStoredState> = _uiStateFlow

    private val eventsChannel: Channel<PreselectedStoredEvent> = bufferedChannel()
    val eventsFlow: Flow<PreselectedStoredEvent> = eventsChannel.receiveAsFlow()

    private var componentState: PaymentComponentState<*>? = null

    private fun getInitialUIState(): PreselectedStoredState {
        val storedPaymentMethodModel = storedPaymentMethod.mapStoredModel(
            dropInConfiguration.isRemovingStoredPaymentMethodsEnabled,
            dropInConfiguration.environment,
        )
        return PreselectedStoredState(
            storedPaymentMethodModel = storedPaymentMethodModel,
            buttonState = ButtonState.ContinueButton()
        )
    }

    fun onPaymentComponentEvent(event: PaymentComponentEvent<*>) {
        when (event) {
            is PaymentComponentEvent.StateChanged -> {
                componentState = event.state
                val buttonState = getButtonState(event.state)
                val newState = _uiStateFlow.value.copy(buttonState = buttonState)
                _uiStateFlow.tryEmit(newState)
            }
            is PaymentComponentEvent.Error -> {
                eventsChannel.trySend(PreselectedStoredEvent.ShowError(event.error))
            }
            is PaymentComponentEvent.Submit -> {
                eventsChannel.trySend(PreselectedStoredEvent.RequestPaymentsCall(event.state))
            }
            is PaymentComponentEvent.ActionDetails -> {
                throw IllegalStateException("This event should not be used in drop-in")
            }
        }
    }

    private fun getButtonState(state: PaymentComponentState<*>): ButtonState {
        return if (!state.isInputValid) {
            // component requires user input -> use it as a Continue button
            ButtonState.ContinueButton()
        } else {
            // component does not require user input -> treat it as a normal Pay button
            ButtonState.PayButton(amount, dropInConfiguration.shopperLocale)
        }
    }

    fun onButtonClicked() {
        val componentState = componentState ?: return

        if (!componentState.isInputValid) {
            // component requires user input -> load the stored component in a new fragment
            eventsChannel.trySend(PreselectedStoredEvent.ShowStoredPaymentScreen)
            return
        }

        // component does not require user input -> we should submit it directly instead of switching to a new screen
        eventsChannel.trySend(PreselectedStoredEvent.SubmitComponent)

        if (!componentState.isValid) {
            // component is not yet ready for submitting -> show a loading indicator until the component indicates that
            // we can make the payments call (by emitting PaymentComponentEvent.Submit)
            val newState = _uiStateFlow.value.copy(buttonState = ButtonState.Loading)
            _uiStateFlow.tryEmit(newState)
        }
    }
}

internal data class PreselectedStoredState(
    val storedPaymentMethodModel: StoredPaymentMethodModel,
    val buttonState: ButtonState,
)

internal sealed class ButtonState {
    object Loading : ButtonState()
    data class ContinueButton(@StringRes val labelResId: Int = R.string.continue_button) : ButtonState()
    data class PayButton(val amount: Amount, val shopperLocale: Locale) : ButtonState()
}

internal sealed class PreselectedStoredEvent {
    object ShowStoredPaymentScreen : PreselectedStoredEvent()
    object SubmitComponent : PreselectedStoredEvent()
    data class RequestPaymentsCall(val state: PaymentComponentState<*>) : PreselectedStoredEvent()
    data class ShowError(val componentError: ComponentError) : PreselectedStoredEvent()
}
