/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 29/9/2022.
 */
package com.adyen.checkout.blik

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.LinearLayout
import com.adyen.checkout.blik.databinding.BlikViewBinding
import com.adyen.checkout.components.base.ComponentDelegate
import com.adyen.checkout.components.extensions.hideError
import com.adyen.checkout.components.extensions.setLocalizedHintFromStyle
import com.adyen.checkout.components.extensions.setLocalizedTextFromStyle
import com.adyen.checkout.components.extensions.showError
import com.adyen.checkout.components.ui.ComponentView
import com.adyen.checkout.components.ui.Validation
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import kotlinx.coroutines.CoroutineScope

internal class BlikView @JvmOverloads constructor(
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

    private val binding: BlikViewBinding = BlikViewBinding.inflate(LayoutInflater.from(context), this)

    private lateinit var localizedContext: Context

    private lateinit var blikDelegate: BlikDelegate

    init {
        orientation = VERTICAL
        val padding = resources.getDimension(R.dimen.standard_margin).toInt()
        setPadding(padding, padding, padding, 0)
    }

    override fun initView(delegate: ComponentDelegate, coroutineScope: CoroutineScope, localizedContext: Context) {
        if (delegate !is BlikDelegate) throw IllegalArgumentException("Unsupported delegate type")
        blikDelegate = delegate

        this.localizedContext = localizedContext
        initLocalizedStrings(localizedContext)

        initBlikCodeInput()
    }

    private fun initLocalizedStrings(localizedContext: Context) {
        binding.textInputLayoutBlikCode.setLocalizedHintFromStyle(
            R.style.AdyenCheckout_Blik_BlikCodeInput,
            localizedContext
        )
        binding.textViewBlikHeader.setLocalizedTextFromStyle(
            R.style.AdyenCheckout_Blik_BlikHeaderTextView,
            localizedContext
        )
    }

    private fun initBlikCodeInput() {
        binding.editTextBlikCode.setOnChangeListener {
            blikDelegate.updateInputData {
                blikCode = binding.editTextBlikCode.rawValue
            }
            binding.textInputLayoutBlikCode.hideError()
        }

        binding.editTextBlikCode.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            val outputData = blikDelegate.outputData
            val blikCodeValidation = outputData.blikCodeField.validation
            if (hasFocus) {
                binding.textInputLayoutBlikCode.hideError()
            } else if (!blikCodeValidation.isValid()) {
                val errorReasonResId = (blikCodeValidation as Validation.Invalid).reason
                binding.textInputLayoutBlikCode.showError(localizedContext.getString(errorReasonResId))
            }
        }
    }

    override fun highlightValidationErrors() {
        Logger.d(TAG, "highlightValidationErrors")
        val blikCodeValidation = blikDelegate.outputData.blikCodeField.validation
        if (!blikCodeValidation.isValid()) {
            binding.textInputLayoutBlikCode.requestFocus()
            val errorReasonResId = (blikCodeValidation as Validation.Invalid).reason
            binding.textInputLayoutBlikCode.showError(localizedContext.getString(errorReasonResId))
        }
    }

    override fun getView(): View = this

    companion object {
        private val TAG = LogUtil.getTag()
    }
}
