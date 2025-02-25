package com.adyen.checkout.card.api

import com.adyen.checkout.card.api.model.AddressItem
import com.adyen.checkout.core.api.HttpClient
import com.adyen.checkout.core.api.getList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AddressService(
    private val httpClient: HttpClient,
) {
    suspend fun getCountries(
        shopperLocale: String
    ): List<AddressItem> = withContext(Dispatchers.IO) {
        httpClient.getList(
            path = "datasets/countries/$shopperLocale.json",
            responseSerializer = AddressItem.SERIALIZER,
        )
    }

    suspend fun getStates(
        shopperLocale: String,
        countryCode: String
    ): List<AddressItem> = withContext(Dispatchers.IO) {
        httpClient.getList(
            path = "datasets/states/$countryCode/$shopperLocale.json",
            responseSerializer = AddressItem.SERIALIZER,
        )
    }
}
