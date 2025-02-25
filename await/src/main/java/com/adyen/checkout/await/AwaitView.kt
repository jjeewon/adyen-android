/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 31/8/2022.
 */
package com.adyen.checkout.await

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.adyen.checkout.await.databinding.AwaitViewBinding
import com.adyen.checkout.components.base.ComponentDelegate
import com.adyen.checkout.components.extensions.setLocalizedTextFromStyle
import com.adyen.checkout.components.image.loadLogo
import com.adyen.checkout.components.ui.ComponentView
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class AwaitView @JvmOverloads constructor(
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

    private val binding: AwaitViewBinding = AwaitViewBinding.inflate(LayoutInflater.from(context), this)

    private lateinit var localizedContext: Context

    private lateinit var delegate: AwaitDelegate

    init {
        orientation = VERTICAL
        val padding = resources.getDimension(R.dimen.standard_double_margin).toInt()
        setPadding(padding, padding, padding, padding)
    }

    override fun initView(delegate: ComponentDelegate, coroutineScope: CoroutineScope, localizedContext: Context) {
        if (delegate !is AwaitDelegate) throw IllegalArgumentException("Unsupported delegate type")

        this.delegate = delegate

        this.localizedContext = localizedContext
        initLocalizedStrings(localizedContext)

        observeDelegate(delegate, coroutineScope)
    }

    private fun initLocalizedStrings(localizedContext: Context) {
        binding.textViewWaitingConfirmation.setLocalizedTextFromStyle(
            R.style.AdyenCheckout_Await_WaitingConfirmationTextView,
            localizedContext
        )
    }

    private fun observeDelegate(delegate: AwaitDelegate, coroutineScope: CoroutineScope) {
        delegate.outputDataFlow
            .onEach { outputDataChanged(it) }
            .launchIn(coroutineScope)
    }

    private fun outputDataChanged(outputData: AwaitOutputData) {
        Logger.d(TAG, "outputDataChanged")

        updateMessageText(outputData.paymentMethodType)
        updateLogo(outputData.paymentMethodType)
    }

    override fun highlightValidationErrors() {
        // No validation required
    }

    private fun updateMessageText(paymentMethodType: String?) {
        getMessageTextResource(paymentMethodType)?.let {
            binding.textViewOpenApp.text = localizedContext.getString(it)
        }
    }

    private fun updateLogo(paymentMethodType: String?) {
        Logger.d(TAG, "updateLogo - $paymentMethodType")
        paymentMethodType?.let { txVariant ->
            binding.imageViewLogo.loadLogo(
                environment = delegate.componentParams.environment,
                txVariant = txVariant,
            )
        }
    }

    @StringRes
    private fun getMessageTextResource(paymentMethodType: String?): Int? {
        return when (paymentMethodType) {
            PaymentMethodTypes.BLIK -> R.string.checkout_await_message_blik
            PaymentMethodTypes.MB_WAY -> R.string.checkout_await_message_mbway
            else -> null
        }
    }

    override fun getView(): View = this

    companion object {
        private val TAG = LogUtil.getTag()
    }
}
