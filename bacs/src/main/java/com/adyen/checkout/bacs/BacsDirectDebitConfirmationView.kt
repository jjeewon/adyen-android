/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 18/11/2021.
 */

package com.adyen.checkout.bacs

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.adyen.checkout.bacs.databinding.BacsDirectDebitConfirmationViewBinding
import com.adyen.checkout.components.base.ComponentDelegate
import com.adyen.checkout.components.extensions.setLocalizedHintFromStyle
import com.adyen.checkout.components.ui.ComponentView
import kotlinx.coroutines.CoroutineScope

internal class BacsDirectDebitConfirmationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayout(
        context,
        attrs,
        defStyleAttr
    ),
    ComponentView {

    private val binding: BacsDirectDebitConfirmationViewBinding =
        BacsDirectDebitConfirmationViewBinding.inflate(LayoutInflater.from(context), this)

    private lateinit var localizedContext: Context

    private lateinit var bacsDelegate: BacsDirectDebitDelegate

    init {
        orientation = VERTICAL
        val padding = resources.getDimension(R.dimen.standard_margin).toInt()
        setPadding(padding, padding, padding, 0)
    }

    override fun initView(delegate: ComponentDelegate, coroutineScope: CoroutineScope, localizedContext: Context) {
        if (delegate !is BacsDirectDebitDelegate) throw IllegalArgumentException("Unsupported delegate type")
        bacsDelegate = delegate

        this.localizedContext = localizedContext
        initLocalizedStrings(localizedContext)

        with(bacsDelegate.outputData) {
            binding.editTextHolderName.setText(holderNameState.value)
            binding.editTextBankAccountNumber.setText(bankAccountNumberState.value)
            binding.editTextSortCode.setText(sortCodeState.value)
            binding.editTextShopperEmail.setText(shopperEmailState.value)
        }
    }

    override fun highlightValidationErrors() {
        // no ops
    }

    private fun initLocalizedStrings(localizedContext: Context) {
        binding.textInputLayoutHolderName.setLocalizedHintFromStyle(
            R.style.AdyenCheckout_Bacs_HolderNameInput,
            localizedContext
        )
        binding.textInputLayoutBankAccountNumber.setLocalizedHintFromStyle(
            R.style.AdyenCheckout_Bacs_AccountNumberInput,
            localizedContext
        )
        binding.textInputLayoutSortCode.setLocalizedHintFromStyle(
            R.style.AdyenCheckout_Bacs_SortCodeInput,
            localizedContext
        )
        binding.textInputLayoutShopperEmail.setLocalizedHintFromStyle(
            R.style.AdyenCheckout_Bacs_ShopperEmailInput,
            localizedContext
        )
    }

    override fun getView(): View = this
}
