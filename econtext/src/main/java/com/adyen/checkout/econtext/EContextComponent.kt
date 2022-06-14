/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 7/6/2022.
 */

package com.adyen.checkout.econtext

import androidx.lifecycle.SavedStateHandle
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.base.BasePaymentComponent
import com.adyen.checkout.components.base.GenericPaymentMethodDelegate
import com.adyen.checkout.components.model.payments.request.EContextPaymentMethod
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.ui.FieldState
import com.adyen.checkout.components.ui.Validation
import com.adyen.checkout.components.util.ValidationUtils

abstract class EContextComponent<EContextPaymentMethodT : EContextPaymentMethod>(
    savedStateHandle: SavedStateHandle,
    genericPaymentMethodDelegate: GenericPaymentMethodDelegate,
    configuration: EContextConfiguration
) : BasePaymentComponent<
    EContextConfiguration,
    EContextInputData,
    EContextOutputData,
    PaymentComponentState<EContextPaymentMethodT>>(
    savedStateHandle,
    genericPaymentMethodDelegate,
    configuration
) {
    override fun onInputDataChanged(inputData: EContextInputData): EContextOutputData {
        return EContextOutputData(
            validateNameField(inputData.firstName),
            validateNameField(inputData.lastName),
            validatePhoneNumber(inputData.mobileNumber),
            validateEmailAddress(inputData.emailAddress)
        )
    }

    private fun validateNameField(input: String): FieldState<String> {
        val validation = if (input.isNotBlank()) {
            Validation.Valid
        } else {
            Validation.Invalid(android.R.string.copy) // TODO translations
        }
        return FieldState(input, validation)
    }

    private fun validatePhoneNumber(phoneNumber: String): FieldState<String> {
        val validation = if (phoneNumber.isNotEmpty() && ValidationUtils.isPhoneNumberValid(phoneNumber)) {
            Validation.Valid
        } else {
            Validation.Invalid(android.R.string.copy) // TODO translations
        }
        return FieldState(phoneNumber, validation)
    }

    private fun validateEmailAddress(emailAddress: String): FieldState<String> {
        val validation = if (emailAddress.isNotEmpty() && ValidationUtils.isEmailValid(emailAddress)) {
            Validation.Valid
        } else {
            Validation.Invalid(android.R.string.copy) // TODO translations
        }
        return FieldState(emailAddress, validation)
    }

    override fun createComponentState(): PaymentComponentState<EContextPaymentMethodT> {
        val eContextPaymentMethod = instantiatePaymentMethod().apply {
            type = paymentMethodDelegate.getPaymentMethodType()
            firstName = outputData?.firstNameState?.value
            lastName = outputData?.lastNameState?.value
            telephoneNumber = outputData?.phoneNumberState?.value
            shopperEmail = outputData?.emailAddressState?.value
        }
        val isInputValid = outputData?.isValid == true
        val paymentComponentData = PaymentComponentData<EContextPaymentMethodT>().apply {
            paymentMethod = eContextPaymentMethod
        }
        return PaymentComponentState(paymentComponentData, isInputValid, true)
    }

    protected abstract fun instantiatePaymentMethod(): EContextPaymentMethodT
}
