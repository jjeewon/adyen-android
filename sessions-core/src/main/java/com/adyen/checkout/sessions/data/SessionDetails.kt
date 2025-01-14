/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by ozgur on 9/1/2023.
 */

package com.adyen.checkout.sessions.data

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.adyen.checkout.components.model.payments.Amount
import com.adyen.checkout.sessions.model.SessionModel
import com.adyen.checkout.sessions.model.setup.SessionSetupResponse
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class SessionDetails(
    val id: String,
    val sessionData: String,
    val amount: Amount,
    val expiresAt: String,
    val returnUrl: String
) : Parcelable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SessionSetupResponse.mapToDetails(): SessionDetails {
    return SessionDetails(
        id = id,
        sessionData = sessionData,
        amount = amount ?: Amount.EMPTY,
        expiresAt = expiresAt,
        returnUrl = returnUrl,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SessionDetails.mapToModel(): SessionModel {
    return SessionModel(
        id = id,
        sessionData = sessionData,
    )
}
