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
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.model.payments.request.EContextPaymentMethod
import com.adyen.checkout.components.ui.view.AdyenLinearLayout

abstract class EContextView<
    EContextPaymentMethodT : EContextPaymentMethod,
    EContextComponentT : EContextComponent<EContextPaymentMethodT>> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AdyenLinearLayout<EContextOutputData,
    EContextConfiguration,
    PaymentComponentState<EContextPaymentMethodT>,
    EContextComponentT>(context, attrs, defStyleAttr) {

    // TODO
}
