/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 23/9/2022.
 */

package com.adyen.checkout.adyen3ds2

import android.content.Context
import android.util.AttributeSet
import com.adyen.checkout.components.ui.ComponentView
import com.adyen.checkout.components.ui.PaymentInProgressView
import com.adyen.checkout.components.ui.ViewProvider
import com.adyen.checkout.components.ui.view.ComponentViewType

internal object Adyen3DS2ViewProvider : ViewProvider {

    override fun getView(
        viewType: ComponentViewType,
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ): ComponentView = when (viewType) {
        Adyen3DS2ComponentViewType -> PaymentInProgressView(context, attrs, defStyleAttr)
        else -> throw IllegalStateException("Unsupported view type")
    }
}

internal object Adyen3DS2ComponentViewType : ComponentViewType {
    override val viewProvider: ViewProvider = Adyen3DS2ViewProvider
}
