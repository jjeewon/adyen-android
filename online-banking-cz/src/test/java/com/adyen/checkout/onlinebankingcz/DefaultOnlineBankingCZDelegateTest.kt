/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by atef on 14/9/2022.
 */

package com.adyen.checkout.onlinebankingcz

import android.content.Context
import app.cash.turbine.test
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.payments.request.OnlineBankingCZPaymentMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
internal class DefaultOnlineBankingCZDelegateTest {

    private lateinit var delegate: OnlineBankingDelegate<OnlineBankingCZPaymentMethod>

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var pdfOpener: PdfOpener

    @BeforeEach
    fun setup() {
        delegate = DefaultOnlineBankingCZDelegate(
            pdfOpener = pdfOpener,
            paymentMethod = PaymentMethod(),
            paymentMethodFactory = { OnlineBankingCZPaymentMethod() }
        )
    }

    @Nested
    @DisplayName("when input data changes and")
    inner class InputDataChangedTest {

        @Test
        fun `selectedIssuer is null, then output should be null`() = runTest {
            delegate.outputDataFlow.test {
                val inputData = OnlineBankingInputData(null)

                delegate.onInputDataChanged(inputData)

                with(requireNotNull(expectMostRecentItem())) {
                    assertNull(selectedIssuer)
                }
            }
        }

        @Test
        fun `selectedIssuer is null, then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                val inputData = OnlineBankingInputData(null)

                delegate.onInputDataChanged(inputData)

                with(requireNotNull(expectMostRecentItem())) {
                    assertNull(selectedIssuer)
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `selectedIssuer is valid, then output should be valid`() = runTest {
            delegate.outputDataFlow.test {
                val model = OnlineBankingModel(id = "id", name = "test")
                val input = OnlineBankingInputData(model)

                delegate.onInputDataChanged(input)

                with(requireNotNull(expectMostRecentItem())) {
                    assertEquals("test", selectedIssuer?.name)
                    assertEquals("id", selectedIssuer?.id)
                    assertTrue(isValid)
                }
            }
        }

        @Test
        fun `selectedIssuer is null, then component state should be invalid`() = runTest {
            delegate.componentStateFlow.test {
                val input = OnlineBankingInputData()

                delegate.onInputDataChanged(input)

                with(requireNotNull(expectMostRecentItem())) {
                    assertEquals("", data.paymentMethod?.issuer)
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `selectIssuer is valid, then component state should be valid`() = runTest {
            delegate.componentStateFlow.test {
                val model = OnlineBankingModel(id = "issuer-id", name = "issuer-name")
                val input = OnlineBankingInputData(model)

                delegate.onInputDataChanged(input)

                with(requireNotNull(expectMostRecentItem())) {
                    assertEquals("issuer-id", data.paymentMethod?.issuer)
                    assertTrue(isValid)
                }
            }
        }
    }

    @Nested
    @DisplayName("when creating component state and")
    inner class CreateComponentStateTest {
        @Test
        fun `output is invalid, then component state should be invalid`() = runTest {
            delegate.componentStateFlow.test {
                val output = OnlineBankingOutputData(null)

                delegate.createComponentState(output)

                with(requireNotNull(expectMostRecentItem())) {
                    assertFalse(isInputValid)
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `output is valid, then component state should be valid`() = runTest {
            delegate.componentStateFlow.test {
                val model = OnlineBankingModel(id = "issuer-id", name = "issuer-name")
                val output = OnlineBankingOutputData(model)

                delegate.createComponentState(output)

                with(requireNotNull(expectMostRecentItem())) {
                    assertEquals("issuer-id", data.paymentMethod?.issuer)
                    assertTrue(isInputValid)
                    assertTrue(isValid)
                }
            }
        }
    }

    @Nested
    @DisplayName("when trying pdf opener and it")
    inner class PdfOpenerTest {
        @Test
        fun `successfully open pdf`() {
            val url = URL

            delegate.openPdf(mockContext, url)

            verify(pdfOpener).open(mockContext, url)
            assertNotNull(url)
        }

        @Test
        fun `failed to open pdf and throw an exception`() {
            val url = URL
            whenever(pdfOpener.open(mockContext, url)) doThrow IllegalStateException("failed")

            delegate.openPdf(mockContext, url)

            assertThrows<IllegalStateException> { pdfOpener.open(mockContext, url) }
            assertNotNull(url)
        }
    }

    companion object {
        private const val URL = "any-url"
    }
}
