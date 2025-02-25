/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 17/3/2022.
 */
package com.adyen.checkout.sessions.model.orders

import com.adyen.checkout.core.exception.ModelSerializationException
import com.adyen.checkout.core.model.ModelObject
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

@Parcelize
data class SessionOrderResponse(
    val sessionData: String,
    val orderData: String,
    val pspReference: String
) : ModelObject() {

    companion object {
        private const val SESSION_DATA = "sessionData"
        private const val ORDER_DATA = "orderData"
        private const val PSP_REFERENCE = "pspReference"

        @JvmField
        val SERIALIZER: Serializer<SessionOrderResponse> = object : Serializer<SessionOrderResponse> {
            override fun serialize(modelObject: SessionOrderResponse): JSONObject {
                return JSONObject().apply {
                    try {
                        putOpt(SESSION_DATA, modelObject.sessionData)
                        putOpt(ORDER_DATA, modelObject.orderData)
                        putOpt(PSP_REFERENCE, modelObject.pspReference)
                    } catch (e: JSONException) {
                        throw ModelSerializationException(SessionOrderResponse::class.java, e)
                    }
                }
            }

            override fun deserialize(jsonObject: JSONObject): SessionOrderResponse {
                return SessionOrderResponse(
                    sessionData = jsonObject.optString(SESSION_DATA),
                    orderData = jsonObject.optString(ORDER_DATA),
                    pspReference = jsonObject.optString(PSP_REFERENCE)
                )
            }
        }
    }
}
