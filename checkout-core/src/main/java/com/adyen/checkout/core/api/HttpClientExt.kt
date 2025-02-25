/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by oscars on 11/4/2022.
 */

package com.adyen.checkout.core.api

import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.core.model.ModelObject
import com.adyen.checkout.core.model.ModelUtils
import com.adyen.checkout.core.model.toStringPretty
import org.json.JSONArray
import org.json.JSONObject

private val TAG = LogUtil.getTag()

suspend fun <T : ModelObject> HttpClient.get(
    path: String,
    responseSerializer: ModelObject.Serializer<T>,
    queryParameters: Map<String, String> = emptyMap(),
): T {
    Logger.d(TAG, "GET - $path")

    val result = this.get(path, queryParameters)
    val resultJson = JSONObject(String(result, Charsets.UTF_8))

    Logger.v(TAG, "response - ${resultJson.toStringPretty()}")

    return responseSerializer.deserialize(resultJson)
}

suspend fun <T : ModelObject> HttpClient.getList(
    path: String,
    responseSerializer: ModelObject.Serializer<T>,
    queryParameters: Map<String, String> = emptyMap(),
): List<T> {
    Logger.d(TAG, "GET - $path")

    val result = this.get(path, queryParameters)
    val resultJson = JSONArray(String(result, Charsets.UTF_8))

    Logger.v(TAG, "response - ${resultJson.toStringPretty()}")

    return ModelUtils.deserializeOptList(resultJson, responseSerializer).orEmpty()
}

suspend fun <T : ModelObject, R : ModelObject> HttpClient.post(
    path: String,
    body: T,
    requestSerializer: ModelObject.Serializer<T>,
    responseSerializer: ModelObject.Serializer<R>,
    queryParameters: Map<String, String> = emptyMap(),
): R {
    Logger.d(TAG, "POST - $path")

    val requestJson = requestSerializer.serialize(body)

    Logger.v(TAG, "request - ${requestJson.toStringPretty()}")

    val result = this.post(path, requestJson.toString(), queryParameters)
    val resultJson = JSONObject(String(result, Charsets.UTF_8))

    Logger.v(TAG, "response - ${resultJson.toStringPretty()}")

    return responseSerializer.deserialize(resultJson)
}
