/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 11/4/2022.
 */

package com.adyen.checkout.components.model

import com.adyen.checkout.core.exception.ModelSerializationException
import com.adyen.checkout.core.model.ModelObject
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

@Parcelize
data class PublicKeyResponse(
    val publicKey: String
) : ModelObject() {

    companion object {

        private const val PUBLIC_KEY = "publicKey"

        @JvmField
        val SERIALIZER: Serializer<PublicKeyResponse> = object : Serializer<PublicKeyResponse> {
            override fun serialize(modelObject: PublicKeyResponse): JSONObject {
                val jsonObject = JSONObject()
                try {
                    jsonObject.putOpt(PUBLIC_KEY, modelObject.publicKey)
                } catch (e: JSONException) {
                    throw ModelSerializationException(PublicKeyResponse::class.java, e)
                }
                return jsonObject
            }

            override fun deserialize(jsonObject: JSONObject): PublicKeyResponse {
                return try {
                    PublicKeyResponse(
                        publicKey = jsonObject.optString(PUBLIC_KEY)
                    )
                } catch (e: JSONException) {
                    throw ModelSerializationException(PublicKeyResponse::class.java, e)
                }
            }
        }
    }
}
