/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 8/8/2022.
 */

package com.adyen.checkout.card.test

import androidx.annotation.RestrictTo
import com.adyen.checkout.card.api.model.Brand
import com.adyen.checkout.card.data.CardType
import com.adyen.checkout.card.data.DetectedCardType
import com.adyen.checkout.card.repository.DetectCardTypeRepository
import com.adyen.checkout.card.test.TestDetectCardTypeRepository.TestDetectedCardType.DETECTED_LOCALLY
import com.adyen.checkout.card.test.TestDetectCardTypeRepository.TestDetectedCardType.DUAL_BRANDED
import com.adyen.checkout.card.test.TestDetectCardTypeRepository.TestDetectedCardType.EMPTY
import com.adyen.checkout.card.test.TestDetectCardTypeRepository.TestDetectedCardType.ERROR
import com.adyen.checkout.card.test.TestDetectCardTypeRepository.TestDetectedCardType.FETCHED_FROM_NETWORK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Test implementation of [DetectCardTypeRepository]. This class should never be used except in test code.
 */
// TODO move to test fixtures once it becomes supported on Android
@RestrictTo(RestrictTo.Scope.TESTS)
internal class TestDetectCardTypeRepository : DetectCardTypeRepository {

    private val _detectedCardTypesFlow: MutableSharedFlow<List<DetectedCardType>> =
        MutableSharedFlow(extraBufferCapacity = 1)
    override val detectedCardTypesFlow: Flow<List<DetectedCardType>> = _detectedCardTypesFlow

    var detectionResult: TestDetectedCardType = DETECTED_LOCALLY

    @Suppress("LongParameterList")
    override fun detectCardType(
        cardNumber: String,
        publicKey: String?,
        supportedCardTypes: List<CardType>,
        clientKey: String,
        coroutineScope: CoroutineScope,
    ) {
        val detectedCardTypes = when (detectionResult) {
            ERROR -> null
            DETECTED_LOCALLY -> getDetectedCardTypesLocal(supportedCardTypes)
            FETCHED_FROM_NETWORK -> getDetectedCardTypesNetwork(supportedCardTypes)
            DUAL_BRANDED -> getDetectedCardTypesDualBranded(supportedCardTypes)
            EMPTY -> emptyList()
        } ?: return

        _detectedCardTypesFlow.tryEmit(detectedCardTypes)
    }

    enum class TestDetectedCardType {
        ERROR,
        DETECTED_LOCALLY,
        FETCHED_FROM_NETWORK,
        DUAL_BRANDED,
        EMPTY,
    }

    fun getDetectedCardTypesLocal(supportedCardTypes: List<CardType>): List<DetectedCardType> {
        val cardType = CardType.VISA
        return listOf(
            DetectedCardType(
                cardType = cardType,
                isReliable = false,
                enableLuhnCheck = true,
                cvcPolicy = Brand.FieldPolicy.REQUIRED,
                expiryDatePolicy = Brand.FieldPolicy.REQUIRED,
                isSupported = supportedCardTypes.contains(cardType),
                panLength = null,
            )
        )
    }

    fun getDetectedCardTypesNetwork(supportedCardTypes: List<CardType>): List<DetectedCardType> {
        val cardType = CardType.MASTERCARD
        return listOf(
            DetectedCardType(
                cardType = cardType,
                isReliable = true,
                enableLuhnCheck = true,
                cvcPolicy = Brand.FieldPolicy.REQUIRED,
                expiryDatePolicy = Brand.FieldPolicy.REQUIRED,
                isSupported = supportedCardTypes.contains(cardType),
                panLength = 16,
            )
        )
    }

    fun getDetectedCardTypesDualBranded(supportedCardTypes: List<CardType>): List<DetectedCardType> {
        val cardTypeFirst = CardType.BCMC
        val cardTypeSecond = CardType.MAESTRO
        return listOf(
            DetectedCardType(
                cardType = cardTypeFirst,
                isReliable = true,
                enableLuhnCheck = true,
                cvcPolicy = Brand.FieldPolicy.HIDDEN,
                expiryDatePolicy = Brand.FieldPolicy.REQUIRED,
                isSupported = supportedCardTypes.contains(cardTypeFirst),
                panLength = 16,
            ),
            DetectedCardType(
                cardType = cardTypeSecond,
                isReliable = true,
                enableLuhnCheck = false,
                cvcPolicy = Brand.FieldPolicy.OPTIONAL,
                expiryDatePolicy = Brand.FieldPolicy.HIDDEN,
                isSupported = supportedCardTypes.contains(cardTypeSecond),
                panLength = 16,
            )
        )
    }
}
