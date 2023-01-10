/*
 * Copyright (c) 2023 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by atef on 10/1/2023.
 */

package com.adyen.checkout.qrcode

sealed class QrCodeUIEvent {
    sealed class QrImageDownloadedResult : QrCodeUIEvent() {
        object Success : QrImageDownloadedResult()
        data class Failure(val throwable: Throwable) : QrImageDownloadedResult()
    }
}
