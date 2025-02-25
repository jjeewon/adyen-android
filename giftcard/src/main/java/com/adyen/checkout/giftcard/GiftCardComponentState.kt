/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 11/11/2021.
 */
package com.adyen.checkout.giftcard

import android.os.Parcelable
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.model.payments.request.GiftCardPaymentMethod
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import kotlinx.parcelize.Parcelize

/**
 * PaymentComponentState for GiftCardComponent with additional data.
 */
@Parcelize
class GiftCardComponentState(
    val paymentComponentData: PaymentComponentData<GiftCardPaymentMethod>,
    override val isInputValid: Boolean,
    override val isReady: Boolean,
    val lastFourDigits: String?
) : PaymentComponentState<GiftCardPaymentMethod>(paymentComponentData, isInputValid, isReady), Parcelable
