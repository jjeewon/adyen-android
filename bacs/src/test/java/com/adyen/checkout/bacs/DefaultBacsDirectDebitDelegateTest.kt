/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by atef on 18/8/2022.
 */

package com.adyen.checkout.bacs

import app.cash.turbine.test
import com.adyen.checkout.components.analytics.AnalyticsRepository
import com.adyen.checkout.components.base.ButtonComponentParamsMapper
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.repository.PaymentObserverRepository
import com.adyen.checkout.components.ui.FieldState
import com.adyen.checkout.components.ui.SubmitHandler
import com.adyen.checkout.components.ui.Validation
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.core.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
internal class DefaultBacsDirectDebitDelegateTest(
    @Mock private val analyticsRepository: AnalyticsRepository,
) {

    private lateinit var delegate: DefaultBacsDirectDebitDelegate

    @BeforeEach
    fun beforeEach() {
        delegate = createBacsDelegate()
        Logger.setLogcatLevel(Logger.NONE)
    }

    @Nested
    @DisplayName("when input data changes and")
    inner class InputDataChangedTest {

        @Test
        fun `account holder is empty, then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = ""
                    bankAccountNumber = "12345678"
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `bank account number length more than 8, then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "1234567890"
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `bank account number length less than 8, then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "1234"
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `bank account number is empty, then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = ""
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `sort code is empty, then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = ""
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `sort code length more than 6 , then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = "12345678"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `sort code length less than 6 , then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = "123"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `shopper email is invalid , then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = "123456"
                    shopperEmail = "test@adyen"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `amount consent check false , then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = false
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `account consent check false , then output should be invalid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = false
                }

                with(expectMostRecentItem()) {
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `input data is valid then output should be valid`() = runTest {
            delegate.outputDataFlow.test {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                }

                with(expectMostRecentItem()) {
                    assertTrue(isValid)
                }
            }
        }
    }

    @Nested
    @DisplayName("when setMode is called")
    inner class SetModeTest {

        @Test
        fun `with INPUT parameter while current mode is also INPUT, then no value should be emitted`() = runTest {
            delegate.updateInputData { mode = BacsDirectDebitMode.INPUT }
            delegate._viewFlow.value = BacsComponentViewType.INPUT

            delegate.viewFlow.test {
                awaitItem()

                delegate.setMode(BacsDirectDebitMode.INPUT)

                expectNoEvents()
            }
        }

        @Test
        fun `with CONFIRMATION parameter while current mode is also CONFIRMATION, then no value should be emitted`() =
            runTest {
                delegate.updateInputData { mode = BacsDirectDebitMode.CONFIRMATION }
                delegate._viewFlow.value = BacsComponentViewType.CONFIRMATION

                delegate.viewFlow.test {
                    awaitItem()

                    delegate.setMode(BacsDirectDebitMode.CONFIRMATION)

                    expectNoEvents()
                }
            }

        @Test
        fun `with INPUT parameter while current mode is CONFIRMATION, then INPUT should be emitted`() = runTest {
            delegate.updateInputData { mode = BacsDirectDebitMode.CONFIRMATION }
            delegate._viewFlow.value = BacsComponentViewType.CONFIRMATION

            delegate.viewFlow.test {
                awaitItem()

                delegate.setMode(BacsDirectDebitMode.INPUT)

                assertEquals(BacsComponentViewType.INPUT, expectMostRecentItem())
            }
        }

        @Test
        fun `with CONFIRMATION parameter while current mode is INPUT and input data is valid, then CONFIRMATION should be emitted`() =
            runTest {
                delegate.updateInputData {
                    holderName = "test"
                    bankAccountNumber = "12345678"
                    sortCode = "123456"
                    shopperEmail = "test@adyen.com"
                    isAmountConsentChecked = true
                    isAccountConsentChecked = true
                    mode = BacsDirectDebitMode.INPUT
                }
                delegate._viewFlow.value = BacsComponentViewType.INPUT

                delegate.viewFlow.test {
                    awaitItem()

                    delegate.setMode(BacsDirectDebitMode.CONFIRMATION)

                    assertEquals(BacsComponentViewType.CONFIRMATION, expectMostRecentItem())
                }
            }

        @Test
        fun `with CONFIRMATION parameter while current mode is INPUT and input data is invalid, then no value should be emitted`() =
            runTest {
                delegate.updateInputData { mode = BacsDirectDebitMode.INPUT }
                delegate._viewFlow.value = BacsComponentViewType.INPUT

                delegate.viewFlow.test {
                    awaitItem()

                    delegate.setMode(BacsDirectDebitMode.CONFIRMATION)

                    expectNoEvents()
                }
            }
    }

    @Nested
    @DisplayName("when creating component state and")
    inner class CreateComponentStateTest {
        @Test
        fun `input is invalid, then component state should be invalid`() = runTest {
            delegate.componentStateFlow.test {
                delegate.updateComponentState(
                    BacsDirectDebitOutputData(
                        holderNameState = FieldState("test", Validation.Invalid(R.string.bacs_holder_name_invalid)),
                        bankAccountNumberState = FieldState(
                            "12345678",
                            Validation.Invalid(R.string.bacs_account_number_invalid)
                        ),
                        sortCodeState = FieldState("123456", Validation.Invalid(R.string.bacs_sort_code_invalid)),
                        shopperEmailState = FieldState("test@adyen.com", Validation.Valid),
                        isAmountConsentChecked = true,
                        isAccountConsentChecked = false,
                        mode = BacsDirectDebitMode.CONFIRMATION,

                        )
                )

                with(expectMostRecentItem()) {
                    assertFalse(isInputValid)
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `input is valid and mode is INPUT, then component state should be invalid`() = runTest {
            delegate.componentStateFlow.test {
                delegate.updateComponentState(
                    BacsDirectDebitOutputData(
                        holderNameState = FieldState("test", Validation.Valid),
                        bankAccountNumberState = FieldState("12345678", Validation.Valid),
                        sortCodeState = FieldState("123456", Validation.Valid),
                        shopperEmailState = FieldState("test@adyen.com", Validation.Valid),
                        isAmountConsentChecked = true,
                        isAccountConsentChecked = true,
                        mode = BacsDirectDebitMode.INPUT,

                        )
                )

                with(expectMostRecentItem()) {
                    assertTrue(isInputValid)
                    assertFalse(isValid)
                }
            }
        }

        @Test
        fun `input is valid and mode is  CONFIRMATION, then component state should be valid`() = runTest {
            delegate.componentStateFlow.test {
                delegate.updateComponentState(
                    BacsDirectDebitOutputData(
                        holderNameState = FieldState("test", Validation.Valid),
                        bankAccountNumberState = FieldState("12345678", Validation.Valid),
                        sortCodeState = FieldState("123456", Validation.Valid),
                        shopperEmailState = FieldState("test@adyen.com", Validation.Valid),
                        isAmountConsentChecked = true,
                        isAccountConsentChecked = true,
                        mode = BacsDirectDebitMode.CONFIRMATION,
                    )
                )

                with(expectMostRecentItem()) {
                    assertTrue(isInputValid)
                    assertTrue(isValid)
                }
            }
        }
    }

    @Test
    fun `when delegate is initialized then analytics event is sent`() = runTest {
        delegate.initialize(CoroutineScope(UnconfinedTestDispatcher()))
        verify(analyticsRepository).sendAnalyticsEvent()
    }

    @Nested
    inner class SubmitButtonVisibilityTest {

        @Test
        fun `when submit button is configured to be hidden, then it should not show`() {
            delegate = createBacsDelegate(
                configuration = getDefaultBacsConfigurationBuilder()
                    .setSubmitButtonVisible(false)
                    .build()
            )

            assertFalse(delegate.shouldShowSubmitButton())
        }

        @Test
        fun `when submit button is configured to be visible, then it should show`() {
            delegate = createBacsDelegate(
                configuration = getDefaultBacsConfigurationBuilder()
                    .setSubmitButtonVisible(true)
                    .build()
            )

            assertTrue(delegate.shouldShowSubmitButton())
        }
    }

    private fun createBacsDelegate(
        configuration: BacsDirectDebitConfiguration = getDefaultBacsConfigurationBuilder().build()
    ) = DefaultBacsDirectDebitDelegate(
        observerRepository = PaymentObserverRepository(),
        componentParams = ButtonComponentParamsMapper(null).mapToParams(configuration),
        paymentMethod = PaymentMethod(),
        analyticsRepository = analyticsRepository,
        submitHandler = SubmitHandler()
    )

    private fun getDefaultBacsConfigurationBuilder() = BacsDirectDebitConfiguration.Builder(
        Locale.US,
        Environment.TEST,
        TEST_CLIENT_KEY
    )

    companion object {
        private const val TEST_CLIENT_KEY = "test_qwertyuiopasdfghjklzxcvbnmqwerty"
    }
}
