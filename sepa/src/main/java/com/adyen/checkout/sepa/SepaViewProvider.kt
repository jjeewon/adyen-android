/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 5/9/2022.
 */

package com.adyen.checkout.sepa

import android.content.Context
import android.util.AttributeSet
import com.adyen.checkout.components.ui.ComponentView
import com.adyen.checkout.components.ui.ViewProvider
import com.adyen.checkout.components.ui.view.AmountButtonComponentViewType
import com.adyen.checkout.components.ui.view.ButtonComponentViewType
import com.adyen.checkout.components.ui.view.ComponentViewType

internal object SepaViewProvider : ViewProvider {

    override fun getView(
        viewType: ComponentViewType,
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ): ComponentView {
        return when (viewType) {
            SepaComponentViewType -> SepaView(context, attrs, defStyleAttr)
            else -> throw IllegalArgumentException("Unsupported view type")
        }
    }
}

internal object SepaComponentViewType : AmountButtonComponentViewType {
    override val viewProvider: ViewProvider = SepaViewProvider
    override val buttonTextResId: Int = ButtonComponentViewType.DEFAULT_BUTTON_TEXT_RES_ID
}
