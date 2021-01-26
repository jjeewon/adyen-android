/*
 * Copyright (c) 2020 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 17/12/2020.
 */

package com.adyen.checkout.card

import androidx.lifecycle.viewModelScope
import com.adyen.checkout.card.api.BinLookupConnection
import com.adyen.checkout.card.data.CardType
import com.adyen.checkout.card.data.ExpiryDate
import com.adyen.checkout.card.model.BinLookupRequest
import com.adyen.checkout.card.model.BinLookupResponse
import com.adyen.checkout.components.StoredPaymentComponentProvider
import com.adyen.checkout.components.api.suspendedCall
import com.adyen.checkout.components.base.BasePaymentComponent
import com.adyen.checkout.components.model.payments.request.CardPaymentMethod
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.core.exception.ComponentException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.cse.CardEncrypter
import com.adyen.checkout.cse.EncryptedCard
import com.adyen.checkout.cse.UnencryptedCard
import com.adyen.checkout.cse.exception.EncryptionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.UUID

private val TAG = LogUtil.getTag()

private val PAYMENT_METHOD_TYPES = arrayOf(PaymentMethodTypes.SCHEME)
private const val BIN_VALUE_LENGTH = 6

@Suppress("TooManyFunctions")
class CardComponent private constructor(
    private val cardDelegate: CardDelegate,
    cardConfiguration: CardConfiguration
) : BasePaymentComponent<CardConfiguration, CardInputData, CardOutputData, CardComponentState>(cardDelegate, cardConfiguration) {

    var filteredSupportedCards: List<CardType> = emptyList()
        private set
    private var storedPaymentInputData: CardInputData? = null
    private var publicKey = ""

    init {
        viewModelScope.launch {
            publicKey = cardDelegate.fetchPublicKey()
            if (publicKey.isEmpty()) {
                notifyException(ComponentException("Unable to fetch publicKey."))
            }
        }
    }

    constructor(storedCardDelegate: StoredCardDelegate, cardConfiguration: CardConfiguration) : this(
        storedCardDelegate as CardDelegate,
        cardConfiguration
    ) {
        storedPaymentInputData = storedCardDelegate.getStoredCardInputData()

        val cardType = storedCardDelegate.getCardType()
        if (cardType != null) {
            val storedCardType: MutableList<CardType> = ArrayList()
            storedCardType.add(cardType)
            filteredSupportedCards = Collections.unmodifiableList(storedCardType)
        }

        // TODO: 09/12/2020 move this logic to base component, maybe create the inputdata from the delegate?
        if (!requiresInput()) {
            inputDataChanged(CardInputData())
        }
    }

    constructor(cardDelegate: NewCardDelegate, cardConfiguration: CardConfiguration) : this(
        cardDelegate as CardDelegate,
        cardConfiguration
    )

    override fun requiresInput(): Boolean {
        return cardDelegate.requiresInput()
    }

    override fun getSupportedPaymentMethodTypes(): Array<String> {
        return PAYMENT_METHOD_TYPES
    }

    override fun onInputDataChanged(inputData: CardInputData): CardOutputData {
        Logger.v(TAG, "onInputDataChanged")
        if (!isStoredPaymentMethod()) {
            filteredSupportedCards = updateSupportedFilterCards(inputData.cardNumber)
        }
        val cardDelegate = mPaymentMethodDelegate as CardDelegate
        val firstCardType: CardType? = if (filteredSupportedCards.isNotEmpty()) filteredSupportedCards[0] else null

        if (inputData.cardNumber.length == BinLookupConnection.REQUIRED_BIN_SIZE) {
            fetchCardType(inputData.cardNumber)
        }

        return CardOutputData(
            cardDelegate.validateCardNumber(inputData.cardNumber),
            cardDelegate.validateExpiryDate(inputData.expiryDate),
            cardDelegate.validateSecurityCode(inputData.securityCode, firstCardType),
            cardDelegate.validateHolderName(inputData.holderName),
            inputData.isStorePaymentEnable,
            cardDelegate.isCvcHidden()
        )
    }

    private fun fetchCardType(cardNumber: String) {
        viewModelScope.launch {
            val deferredEncryption = async(Dispatchers.Default) {
                CardEncrypter.encryptBin(cardNumber, publicKey)
            }
            try {
                val encryptedBin = deferredEncryption.await()
                val request = BinLookupRequest(encryptedBin, UUID.randomUUID().toString(), getCardTypes())
                val response = BinLookupConnection(request, configuration.environment, configuration.clientKey).suspendedCall()
                cardTypeReceived(response)
            } catch (e: EncryptionException) {
                Logger.e(TAG, "Failed to encrypt BIN", e)
                return@launch
            } catch (e: IOException) {
                Logger.e(TAG, "Failed to call binLookup API.", e)
                return@launch
            }
        }
    }

    private fun cardTypeReceived(binLookupResponse: BinLookupResponse) {
        Logger.d(TAG, "cardBrandReceived")
        val brands = binLookupResponse.brands
        when {
            brands.isNullOrEmpty() -> {
                Logger.d(TAG, "Card brand not found.")
                // TODO: 19/01/2021 Keep regexes prediction and don't apply business rules
            }
            brands.size > 1 -> {
                Logger.d(TAG, "Multiple brands found.")
                // TODO: 19/01/2021 use first brand
            }
            else -> {
                Logger.d(TAG, "Card brand: ${brands.first().brand}")
                val cardType = CardType.getByBrandName(brands.first().brand.orEmpty())
                Logger.d(TAG, "CardType: ${cardType?.name}")
                // TODO: 19/01/2021 trigger brand specific business logic
            }
        }
    }

    private fun getCardTypes(): List<String> {
        return configuration.supportedCardTypes.map { it.txVariant }
    }

    @Suppress("ReturnCount")
    override fun createComponentState(): CardComponentState {
        Logger.v(TAG, "createComponentState")

        val cardPaymentMethod = CardPaymentMethod()
        cardPaymentMethod.type = CardPaymentMethod.PAYMENT_METHOD_TYPE

        val unenctryptedCardBuilder = UnencryptedCard.Builder()
        val outputData = outputData
        val paymentComponentData = PaymentComponentData<CardPaymentMethod>()

        val cardNumber = outputData!!.cardNumberField.value

        val firstCardType: CardType? = if (filteredSupportedCards.isNotEmpty()) filteredSupportedCards[0] else null

        val binValue: String = getBinValueFromCardNumber(cardNumber)

        // If data is not valid we just return empty object, encryption would fail and we don't pass unencrypted data.
        if (!outputData.isValid) {
            return CardComponentState(paymentComponentData, false, firstCardType, binValue)
        }

        val encryptedCard: EncryptedCard
        encryptedCard = try {
            if (!isStoredPaymentMethod()) {
                unenctryptedCardBuilder.setNumber(outputData.cardNumberField.value)
            }
            if (!cardDelegate.isCvcHidden()) {
                unenctryptedCardBuilder.setCvc(outputData.securityCodeField.value)
            }
            val expiryDateResult = outputData.expiryDateField.value
            if (expiryDateResult.expiryYear != ExpiryDate.EMPTY_VALUE && expiryDateResult.expiryMonth != ExpiryDate.EMPTY_VALUE) {
                unenctryptedCardBuilder.setExpiryMonth(expiryDateResult.expiryMonth.toString())
                unenctryptedCardBuilder.setExpiryYear(expiryDateResult.expiryYear.toString())
            }

            CardEncrypter.encryptFields(unenctryptedCardBuilder.build(), publicKey)
        } catch (e: EncryptionException) {
            notifyException(e)
            return CardComponentState(paymentComponentData, false, firstCardType, binValue)
        }

        if (!isStoredPaymentMethod()) {
            cardPaymentMethod.encryptedCardNumber = encryptedCard.encryptedCardNumber
            cardPaymentMethod.encryptedExpiryMonth = encryptedCard.encryptedExpiryMonth
            cardPaymentMethod.encryptedExpiryYear = encryptedCard.encryptedExpiryYear
        } else {
            cardPaymentMethod.storedPaymentMethodId = (mPaymentMethodDelegate as StoredCardDelegate).getId()
        }

        if (!cardDelegate.isCvcHidden()) {
            cardPaymentMethod.encryptedSecurityCode = encryptedCard.encryptedSecurityCode
        }

        if (cardDelegate.isHolderNameRequired()) {
            cardPaymentMethod.holderName = outputData.holderNameField.value
        }

        paymentComponentData.paymentMethod = cardPaymentMethod
        paymentComponentData.setStorePaymentMethod(outputData.isStoredPaymentMethodEnable)
        paymentComponentData.shopperReference = configuration.shopperReference

        return CardComponentState(paymentComponentData, outputData.isValid, firstCardType, binValue)
    }

    fun isStoredPaymentMethod(): Boolean {
        return cardDelegate is StoredCardDelegate
    }

    fun getStoredPaymentInputData(): CardInputData? {
        return storedPaymentInputData
    }

    fun isHolderNameRequire(): Boolean {
        return cardDelegate.isHolderNameRequired()
    }

    fun showStorePaymentField(): Boolean {
        return configuration.isShowStorePaymentFieldEnable
    }

    private fun updateSupportedFilterCards(cardNumber: String?): List<CardType> {
        Logger.d(TAG, "updateSupportedFilterCards")
        if (cardNumber.isNullOrEmpty()) {
            return emptyList()
        }
        val supportedCardTypes = configuration.supportedCardTypes
        val estimateCardTypes = CardType.estimate(cardNumber)
        val filteredCards: MutableList<CardType> = ArrayList()
        for (supportedCard in supportedCardTypes) {
            if (estimateCardTypes.contains(supportedCard)) {
                filteredCards.add(supportedCard)
            }
        }
        return Collections.unmodifiableList(filteredCards)
    }

    private fun getBinValueFromCardNumber(cardNumber: String): String {
        return if (cardNumber.length < BIN_VALUE_LENGTH) cardNumber else cardNumber.substring(0..BIN_VALUE_LENGTH)
    }

    companion object {
        @JvmStatic
        val PROVIDER: StoredPaymentComponentProvider<CardComponent, CardConfiguration> = CardComponentProvider()
    }
}
