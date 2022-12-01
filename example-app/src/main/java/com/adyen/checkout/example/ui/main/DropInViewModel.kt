/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by onurk on 30/11/2022.
 */

package com.adyen.checkout.example.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.checkout.example.data.storage.KeyValueStorage
import com.adyen.checkout.example.repositories.PaymentsRepository
import com.adyen.checkout.example.service.getPaymentMethodRequest
import com.adyen.checkout.example.ui.configuration.CheckoutConfigurationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DropInViewModel @Inject constructor(
    private val paymentsRepository: PaymentsRepository,
    private val keyValueStorage: KeyValueStorage,
    private val checkoutConfigurationProvider: CheckoutConfigurationProvider,
) : ViewModel() {

    private val _viewState = MutableStateFlow<DropInViewState>(DropInViewState.Success)
    val viewState = _viewState.asStateFlow()

    private val _event = MutableSharedFlow<DropInNavigation>(extraBufferCapacity = 1)
    val event = _event.asSharedFlow()

    fun dropInClick() {
        viewModelScope.launch {
            _viewState.emit(DropInViewState.Loading)

            val paymentMethods = getPaymentMethods()
            if (paymentMethods != null) {
                _viewState.emit(DropInViewState.Success)
                val dropInConfiguration = checkoutConfigurationProvider.getDropInConfiguration()
                _event.emit(DropInNavigation.DropIn(paymentMethods, dropInConfiguration))
            } else {
                _viewState.emit(DropInViewState.Error("Something went wrong while fetching payment methods"))
            }
        }
    }

    private suspend fun getPaymentMethods() = paymentsRepository.getPaymentMethods(
        getPaymentMethodRequest(
            merchantAccount = keyValueStorage.getMerchantAccount(),
            shopperReference = keyValueStorage.getShopperReference(),
            amount = keyValueStorage.getAmount(),
            countryCode = keyValueStorage.getCountry(),
            shopperLocale = keyValueStorage.getShopperLocale(),
            splitCardFundingSources = keyValueStorage.isSplitCardFundingSources()
        )
    )
}
