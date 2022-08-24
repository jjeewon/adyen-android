/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by atef on 23/8/2022.
 */

package com.adyen.checkout.components.model.payments.request

import android.os.Parcel
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.core.exception.ModelSerializationException
import com.adyen.checkout.core.model.JsonUtils
import com.adyen.checkout.core.model.getStringOrNull
import org.json.JSONException
import org.json.JSONObject

class OnlineBankingCZPaymentMethod(
    override var type: String? = null,
    override var issuer: String? = null,
) : IssuerListPaymentMethod() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        JsonUtils.writeToParcel(dest, SERIALIZER.serialize(this))
    }

    companion object {
        const val PAYMENT_METHOD_TYPE = PaymentMethodTypes.ONLINE_BANKING_CZ

        @JvmField
        val CREATOR = Creator(OnlineBankingCZPaymentMethod::class.java)

        @JvmField
        val SERIALIZER: Serializer<OnlineBankingCZPaymentMethod> = object : Serializer<OnlineBankingCZPaymentMethod> {
            override fun serialize(modelObject: OnlineBankingCZPaymentMethod): JSONObject {
                return try {
                    JSONObject().apply {
                        putOpt(TYPE, modelObject.type)
                        putOpt(ISSUER, modelObject.issuer)
                    }
                } catch (e: JSONException) {
                    throw ModelSerializationException(OnlineBankingCZPaymentMethod::class.java, e)
                }
            }

            override fun deserialize(jsonObject: JSONObject): OnlineBankingCZPaymentMethod {
                return OnlineBankingCZPaymentMethod(
                    type = jsonObject.getStringOrNull(TYPE),
                    issuer = jsonObject.getStringOrNull(ISSUER)
                )
            }
        }
    }
}