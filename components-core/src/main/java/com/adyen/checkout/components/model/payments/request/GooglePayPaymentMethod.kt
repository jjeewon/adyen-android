/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 4/7/2019.
 */
package com.adyen.checkout.components.model.payments.request

import com.adyen.checkout.core.exception.ModelSerializationException
import com.adyen.checkout.core.model.getStringOrNull
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

@Parcelize
class GooglePayPaymentMethod(
    override var type: String? = null,
    var googlePayToken: String? = null,
    var googlePayCardNetwork: String? = null,
) : PaymentMethodDetails() {

    companion object {
        private const val GOOGLE_PAY_TOKEN = "googlePayToken"
        private const val GOOGLE_PAY_CARD_NETWORK = "googlePayCardNetwork"

        @JvmField
        val SERIALIZER: Serializer<GooglePayPaymentMethod> = object : Serializer<GooglePayPaymentMethod> {
            override fun serialize(modelObject: GooglePayPaymentMethod): JSONObject {
                return try {
                    JSONObject().apply {
                        putOpt(TYPE, modelObject.type)
                        putOpt(GOOGLE_PAY_TOKEN, modelObject.googlePayToken)
                        putOpt(GOOGLE_PAY_CARD_NETWORK, modelObject.googlePayCardNetwork)
                    }
                } catch (e: JSONException) {
                    throw ModelSerializationException(GooglePayPaymentMethod::class.java, e)
                }
            }

            override fun deserialize(jsonObject: JSONObject): GooglePayPaymentMethod {
                return GooglePayPaymentMethod(
                    type = jsonObject.getStringOrNull(TYPE),
                    googlePayToken = jsonObject.getStringOrNull(GOOGLE_PAY_TOKEN),
                    googlePayCardNetwork = jsonObject.getStringOrNull(GOOGLE_PAY_CARD_NETWORK)
                )
            }
        }
    }
}
