/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 7/6/2022.
 */

package com.adyen.checkout.econtext

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.AdapterView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.model.payments.request.EContextPaymentMethod
import com.adyen.checkout.components.ui.Validation
import com.adyen.checkout.components.ui.adapter.CountryAdapter
import com.adyen.checkout.components.ui.adapter.CountryModel
import com.adyen.checkout.components.ui.view.AdyenLinearLayout
import com.adyen.checkout.components.ui.view.AdyenTextInputEditText
import com.adyen.checkout.components.util.CountryUtils
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.econtext.databinding.EcontextViewBinding

private val TAG = LogUtil.getTag()

@Suppress("TooManyFunctions")
abstract class EContextView<
    EContextPaymentMethodT : EContextPaymentMethod,
    EContextComponentT : EContextComponent<EContextPaymentMethodT>> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AdyenLinearLayout<EContextOutputData,
        EContextConfiguration,
        PaymentComponentState<EContextPaymentMethodT>,
        EContextComponentT>(context, attrs, defStyleAttr),
    Observer<EContextOutputData> {

    private val binding: EcontextViewBinding = EcontextViewBinding.inflate(LayoutInflater.from(context), this)

    private val eContextInputData = EContextInputData()
    private var countryAdapter: CountryAdapter? = null


    init {
        orientation = VERTICAL
        val padding = resources.getDimension(R.dimen.standard_margin).toInt()
        setPadding(padding, padding, padding, 0)
    }

    override fun onComponentAttached() {
        // no ops
    }

    override fun initView() {
        initFirstNameInput()
        initLastNameInput()
        initCountryCodeInput()
        initMobileNumberInput()
        initEmailAddressInput()
    }

    override val isConfirmationRequired: Boolean
        get() = true

    override fun highlightValidationErrors() {
        component.outputData?.let {
            var isErrorFocused = false
            val firstNameValidation = it.firstNameState.validation
            if (firstNameValidation is Validation.Invalid) {
                isErrorFocused = true
                binding.editTextFirstName.requestFocus()
                binding.textInputLayoutFirstName.error = localizedContext.getString(firstNameValidation.reason)
            }
            val lastNameValidation = it.lastNameState.validation
            if (lastNameValidation is Validation.Invalid) {
                if (!isErrorFocused) {
                    isErrorFocused = true
                    binding.editTextLastName.requestFocus()
                }
                binding.textInputLayoutLastName.error = localizedContext.getString(lastNameValidation.reason)
            }
            // TODO phone number highlight validation error
            val emailAddressValidation = it.emailAddressState.validation
            if (emailAddressValidation is Validation.Invalid) {
                if (!isErrorFocused) {
                    isErrorFocused = true
                    binding.editTextEmailAddress.requestFocus()
                }
                binding.textInputLayoutEmailAddress.error = localizedContext.getString(emailAddressValidation.reason)
            }
        }
    }

    override fun initLocalizedStrings(localizedContext: Context) {
        binding.textInputLayoutFirstName.setLocalizedHintFromStyle(R.style.AdyenCheckout_EContext_FirstNameInput)
        binding.textInputLayoutLastName.setLocalizedHintFromStyle(R.style.AdyenCheckout_EContext_LastNameInput)
        binding.textInputLayoutMobileNumber.setLocalizedHintFromStyle(R.style.AdyenCheckout_EContext_MobileNumberInput)
        binding.textInputLayoutEmailAddress.setLocalizedHintFromStyle(R.style.AdyenCheckout_EContext_EmailAddressInput)
    }

    override fun observeComponentChanges(lifecycleOwner: LifecycleOwner) {
        component.observeOutputData(lifecycleOwner, this)
    }

    override fun onChanged(outputData: EContextOutputData) {
        Logger.d(TAG, "onChanged")
    }

    private fun notifyInputDataChanged() {
        component.inputDataChanged(eContextInputData)
    }

    private fun initFirstNameInput() {
        val firstNameEditText = binding.editTextFirstName as? AdyenTextInputEditText
        firstNameEditText?.setOnChangeListener {
            eContextInputData.firstName = it.toString()
            notifyInputDataChanged()
            binding.textInputLayoutFirstName.error = null
        }
        firstNameEditText?.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val firstNameValidation = component.outputData?.firstNameState?.validation
            if (hasFocus) {
                binding.textInputLayoutFirstName.error = null
            } else if (firstNameValidation != null && firstNameValidation is Validation.Invalid) {
                binding.textInputLayoutFirstName.error = localizedContext.getString(firstNameValidation.reason)
            }
        }
    }

    private fun initLastNameInput() {
        val lastNameEditText = binding.editTextLastName as? AdyenTextInputEditText
        lastNameEditText?.setOnChangeListener {
            eContextInputData.lastName = it.toString()
            notifyInputDataChanged()
            binding.textInputLayoutLastName.error = null
        }
        lastNameEditText?.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val lastNameValidation = component.outputData?.lastNameState?.validation
            if (hasFocus) {
                binding.textInputLayoutLastName.error = null
            } else if (lastNameValidation != null && lastNameValidation is Validation.Invalid) {
                binding.textInputLayoutLastName.error = localizedContext.getString(lastNameValidation.reason)
            }
        }
    }

    private fun initCountryCodeInput() {
        val countryAutoCompleteTextView = binding.autoCompleteTextViewCountry
        val countries = CountryUtils.getCountries().map {
            CountryModel(
                isoCode = it.isoCode,
                countryName = CountryUtils.getCountryName(it.isoCode, component.configuration.shopperLocale),
                callingCode = it.callingCode,
                emoji = it.emoji
            )
        }
        countryAdapter = CountryAdapter(context, localizedContext).apply {
            setItems(countries)
        }
        countryAutoCompleteTextView.apply {
            inputType = 0
            setAdapter(countryAdapter)
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val country = countryAdapter?.getCountries()?.get(position) ?: return@OnItemClickListener
                onCountrySelected(country)
            }
            countries.firstOrNull()?.let {
                setText(it.toShortString())
                onCountrySelected(it)
            }
        }
    }

    private fun onCountrySelected(country: CountryModel) {
        eContextInputData.countryCode = country.callingCode
        notifyInputDataChanged()
    }

    private fun initMobileNumberInput() {
        val mobileNumberEditText = binding.editTextMobileNumber as? AdyenTextInputEditText
        mobileNumberEditText?.setOnChangeListener {
            eContextInputData.mobileNumber = it.toString()
            notifyInputDataChanged()
            binding.textInputLayoutMobileNumber.error = null
        }
        mobileNumberEditText?.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val phoneNumberValidation = component.outputData?.phoneNumberState?.validation
            if (hasFocus) {
                binding.textInputLayoutMobileNumber.error = null
            } else if (phoneNumberValidation != null && phoneNumberValidation is Validation.Invalid) {
                binding.textInputLayoutMobileNumber.error = localizedContext.getString(phoneNumberValidation.reason)
            }
        }
    }

    private fun initEmailAddressInput() {
        val emailAddressEditText = binding.editTextEmailAddress as? AdyenTextInputEditText
        emailAddressEditText?.setOnChangeListener {
            eContextInputData.emailAddress = it.toString()
            notifyInputDataChanged()
            binding.textInputLayoutEmailAddress.error = null
        }
        emailAddressEditText?.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val emailAddressValidation = component.outputData?.emailAddressState?.validation
            if (hasFocus) {
                binding.textInputLayoutEmailAddress.error = null
            } else if (emailAddressValidation != null && emailAddressValidation is Validation.Invalid) {
                binding.textInputLayoutEmailAddress.error = localizedContext.getString(emailAddressValidation.reason)
            }
        }
    }
}
