/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by atef on 20/9/2022.
 */

package com.adyen.checkout.onlinebankingcore

import com.adyen.checkout.components.base.InputData

data class OnlineBankingInputData(
    var selectedIssuer: OnlineBankingModel? = null,
) : InputData
