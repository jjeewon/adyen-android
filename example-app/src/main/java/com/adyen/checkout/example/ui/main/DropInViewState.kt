/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by onurk on 30/11/2022.
 */

package com.adyen.checkout.example.ui.main

sealed class DropInViewState {
    object Loading : DropInViewState()
    object Success : DropInViewState()
    class Error(val message: String) : DropInViewState()
}
