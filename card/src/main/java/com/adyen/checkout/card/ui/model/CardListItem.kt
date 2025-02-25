/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 6/10/2022.
 */

package com.adyen.checkout.card.ui.model

import com.adyen.checkout.card.data.CardType
import com.adyen.checkout.core.api.Environment

data class CardListItem(
    val cardType: CardType,
    val isDetected: Boolean,
    // We need the environment to load the logo
    val environment: Environment,
)
