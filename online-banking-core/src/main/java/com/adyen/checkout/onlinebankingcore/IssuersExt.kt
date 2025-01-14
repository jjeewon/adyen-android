/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by atef on 20/9/2022.
 */

package com.adyen.checkout.onlinebankingcore

import com.adyen.checkout.components.model.paymentmethods.InputDetail
import com.adyen.checkout.components.model.paymentmethods.Issuer

internal fun List<Issuer>.mapToModel(): List<OnlineBankingModel> =
    this.mapNotNull { issuer ->
        val (id, name, isDisabled) = issuer
        if (!isDisabled && id != null && name != null) {
            OnlineBankingModel(id, name)
        } else {
            null
        }
    }

internal fun List<InputDetail>?.getLegacyIssuers(): List<OnlineBankingModel> =
    this.orEmpty()
        .flatMap { it.items.orEmpty() }
        .mapNotNull { item ->
            val (id, name) = item
            if (id != null && name != null) {
                OnlineBankingModel(id, name)
            } else {
                null
            }
        }
