package com.adyen.checkout.example.ui.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.checkout.card.CardComponent
import com.adyen.checkout.card.CardComponentState
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.ActionComponentEvent
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.core.model.getStringOrNull
import com.adyen.checkout.example.data.storage.KeyValueStorage
import com.adyen.checkout.example.repositories.PaymentsRepository
import com.adyen.checkout.example.service.createPaymentRequest
import com.adyen.checkout.example.service.getPaymentMethodRequest
import com.adyen.checkout.example.ui.card.CardActivity.Companion.RETURN_URL_EXTRA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
internal class CardViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val paymentsRepository: PaymentsRepository,
    private val keyValueStorage: KeyValueStorage,
) : ViewModel() {

    private val _cardViewState = MutableStateFlow<CardViewState>(CardViewState.Loading)
    val cardViewState: Flow<CardViewState> = _cardViewState

    private val _events = MutableSharedFlow<CardEvent>()
    val events: Flow<CardEvent> = _events

    private var cardComponentState: CardComponentState? = null

    fun onCreate() {
        viewModelScope.launch { _cardViewState.emit(fetchPaymentMethods()) }
    }

    private suspend fun fetchPaymentMethods(): CardViewState = withContext(Dispatchers.IO) {
        val paymentMethodResponse = paymentsRepository.getPaymentMethods(
            getPaymentMethodRequest(
                merchantAccount = keyValueStorage.getMerchantAccount(),
                shopperReference = keyValueStorage.getShopperReference(),
                amount = keyValueStorage.getAmount(),
                countryCode = keyValueStorage.getCountry(),
                shopperLocale = keyValueStorage.getShopperLocale(),
                splitCardFundingSources = keyValueStorage.isSplitCardFundingSources()
            )
        )

        val cardPaymentMethod = paymentMethodResponse
            ?.paymentMethods
            ?.firstOrNull { CardComponent.PROVIDER.isPaymentMethodSupported(it) }

        if (cardPaymentMethod == null) CardViewState.Error
        else CardViewState.ShowComponent(cardPaymentMethod)
    }

    fun onPaymentComponentEvent(event: PaymentComponentEvent<CardComponentState>) {
        when (event) {
            is PaymentComponentEvent.StateChanged -> onCardComponentState(event.state)
            is PaymentComponentEvent.Error -> onComponentError(event.error)
        }
    }

    private fun onCardComponentState(state: CardComponentState) {
        cardComponentState = state
    }

    fun onPayClick() {
        val state = cardComponentState
        when {
            state == null -> _cardViewState.tryEmit(CardViewState.Error)
            state.isValid -> makePayment(state.data)
            else -> viewModelScope.launch { _events.emit(CardEvent.Invalid) }
        }
    }

    private fun makePayment(data: PaymentComponentData<*>) {
        _cardViewState.value = CardViewState.Loading

        val paymentComponentData = PaymentComponentData.SERIALIZER.serialize(data)

        viewModelScope.launch(Dispatchers.IO) {
            val paymentRequest = createPaymentRequest(
                paymentComponentData = paymentComponentData,
                shopperReference = keyValueStorage.getShopperReference(),
                amount = keyValueStorage.getAmount(),
                countryCode = keyValueStorage.getCountry(),
                merchantAccount = keyValueStorage.getMerchantAccount(),
                redirectUrl = savedStateHandle.get<String>(RETURN_URL_EXTRA)
                    ?: throw IllegalStateException("Return url should be set"),
                isThreeds2Enabled = keyValueStorage.isThreeds2Enable(),
                isExecuteThreeD = keyValueStorage.isExecuteThreeD()
            )

            handlePaymentResponse(paymentsRepository.paymentsRequestAsync(paymentRequest))
        }
    }

    private suspend fun handlePaymentResponse(json: JSONObject?) {
        json?.let {
            when {
                json.has("action") -> {
                    val action = Action.SERIALIZER.deserialize(json.getJSONObject("action"))
                    handleAction(action)
                }
                else -> _events.emit(CardEvent.PaymentResult("Success: ${json.getStringOrNull("resultCode")}"))
            }
        } ?: _events.emit(CardEvent.PaymentResult("Failed"))
    }

    private suspend fun handleAction(action: Action) {
        val cardAction = when (action.type) {
            "redirect" -> CardAction.Redirect(action)
            "threeDS2" -> CardAction.ThreeDS2(action)
            else -> CardAction.Unsupported
        }

        _events.emit(CardEvent.AdditionalAction(cardAction))
    }

    fun onActionComponentEvent(event: ActionComponentEvent) {
        when (event) {
            is ActionComponentEvent.ActionDetails -> sendPaymentDetails(event.data)
            is ActionComponentEvent.Error -> onComponentError(event.error)
        }
    }

    private fun sendPaymentDetails(actionComponentData: ActionComponentData) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = ActionComponentData.SERIALIZER.serialize(actionComponentData)
            handlePaymentResponse(paymentsRepository.detailsRequestAsync(json))
        }
    }

    private fun onComponentError(error: ComponentError) {
        viewModelScope.launch { _events.emit(CardEvent.PaymentResult("Failed: ${error.errorMessage}")) }
    }
}