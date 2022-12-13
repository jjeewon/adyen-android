/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 15/11/2022.
 */

package com.adyen.checkout.components.base

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.adyen.checkout.core.api.Environment
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ComponentParams : Parcelable {
    val shopperLocale: Locale
    val environment: Environment
    val clientKey: String
    val isAnalyticsEnabled: Boolean
    val isCreatedByDropIn: Boolean
}