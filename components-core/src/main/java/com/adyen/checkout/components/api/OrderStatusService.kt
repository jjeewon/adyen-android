/*
 * Copyright (c) 2021 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 15/11/2021.
 */

package com.adyen.checkout.components.api

import androidx.annotation.RestrictTo
import com.adyen.checkout.components.model.connection.OrderStatusRequest
import com.adyen.checkout.components.model.connection.OrderStatusResponse
import com.adyen.checkout.core.api.HttpClient
import com.adyen.checkout.core.api.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OrderStatusService(
    private val httpClient: HttpClient,
) {

    suspend fun getOrderStatus(
        request: OrderStatusRequest,
        clientKey: String
    ): OrderStatusResponse = withContext(Dispatchers.IO) {
        httpClient.post(
            path = "v1/order/status",
            queryParameters = mapOf("clientKey" to clientKey),
            body = request,
            requestSerializer = OrderStatusRequest.SERIALIZER,
            responseSerializer = OrderStatusResponse.SERIALIZER
        )
    }
}
