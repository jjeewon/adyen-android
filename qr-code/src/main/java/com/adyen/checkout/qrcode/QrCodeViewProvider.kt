/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 9/9/2022.
 */

package com.adyen.checkout.qrcode

import android.content.Context
import android.util.AttributeSet
import com.adyen.checkout.components.ui.ComponentView
import com.adyen.checkout.components.ui.PaymentInProgressView
import com.adyen.checkout.components.ui.ViewProvider
import com.adyen.checkout.components.ui.view.ComponentViewType

internal object QrCodeViewProvider : ViewProvider {

    override fun getView(
        viewType: ComponentViewType,
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ): ComponentView {
        return when (viewType) {
            QrCodeComponentViewType.QR_CODE -> QrCodeView(context, attrs, defStyleAttr)
            QrCodeComponentViewType.REDIRECT -> PaymentInProgressView(context, attrs, defStyleAttr)
            else -> throw IllegalArgumentException("Unsupported view type")
        }
    }
}

internal enum class QrCodeComponentViewType : ComponentViewType {
    QR_CODE, REDIRECT;

    override val viewProvider: ViewProvider = QrCodeViewProvider
}
