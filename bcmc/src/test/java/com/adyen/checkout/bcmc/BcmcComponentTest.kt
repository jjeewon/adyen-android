/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 13/12/2022.
 */

package com.adyen.checkout.bcmc

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.adyen.checkout.action.DefaultActionHandlingComponent
import com.adyen.checkout.action.GenericActionDelegate
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.model.payments.request.CardPaymentMethod
import com.adyen.checkout.components.test.TestComponentViewType
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.test.TestDispatcherExtension
import com.adyen.checkout.test.extensions.invokeOnCleared
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class, TestDispatcherExtension::class)
internal class BcmcComponentTest(
    @Mock private val bcmcDelegate: BcmcDelegate,
    @Mock private val genericActionDelegate: GenericActionDelegate,
    @Mock private val actionHandlingComponent: DefaultActionHandlingComponent,
) {

    private lateinit var component: BcmcComponent

    @BeforeEach
    fun before() {
        whenever(bcmcDelegate.viewFlow) doReturn MutableStateFlow(BcmcComponentViewType)
        whenever(genericActionDelegate.viewFlow) doReturn MutableStateFlow(null)

        component = BcmcComponent(
            bcmcDelegate,
            genericActionDelegate,
            actionHandlingComponent,
        )
        Logger.setLogcatLevel(Logger.NONE)
    }

    @Test
    fun `when component is created then delegates are initialized`() {
        verify(bcmcDelegate).initialize(component.viewModelScope)
        verify(genericActionDelegate).initialize(component.viewModelScope)
    }

    @Test
    fun `when component is cleared then delegates are cleared`() {
        component.invokeOnCleared()

        verify(bcmcDelegate).onCleared()
        verify(genericActionDelegate).onCleared()
    }

    @Test
    fun `when observe is called then observe in delegates is called`() {
        val lifecycleOwner = mock<LifecycleOwner>()
        val callback: (PaymentComponentEvent<PaymentComponentState<CardPaymentMethod>>) -> Unit = {}

        component.observe(lifecycleOwner, callback)

        verify(bcmcDelegate).observe(lifecycleOwner, component.viewModelScope, callback)
        verify(genericActionDelegate).observe(eq(lifecycleOwner), eq(component.viewModelScope), any())
    }

    @Test
    fun `when removeObserver is called then removeObserver in delegates is called`() {
        component.removeObserver()

        verify(bcmcDelegate).removeObserver()
        verify(genericActionDelegate).removeObserver()
    }

    @Test
    fun `when component is initialized then view flow should match bcmc delegate view flow`() = runTest {
        component.viewFlow.test {
            assertEquals(BcmcComponentViewType, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `when bcmc delegate view flow emits a value then component view flow should match that value`() = runTest {
        val bcmcDelegateViewFlow = MutableStateFlow(TestComponentViewType.VIEW_TYPE_1)
        whenever(bcmcDelegate.viewFlow) doReturn bcmcDelegateViewFlow
        component = BcmcComponent(bcmcDelegate, genericActionDelegate, actionHandlingComponent)

        component.viewFlow.test {
            assertEquals(TestComponentViewType.VIEW_TYPE_1, awaitItem())

            bcmcDelegateViewFlow.emit(TestComponentViewType.VIEW_TYPE_2)
            assertEquals(TestComponentViewType.VIEW_TYPE_2, awaitItem())

            expectNoEvents()
        }
    }

    @Test
    fun `when action delegate view flow emits a value then component view flow should match that value`() = runTest {
        val actionDelegateViewFlow = MutableStateFlow(TestComponentViewType.VIEW_TYPE_1)
        whenever(genericActionDelegate.viewFlow) doReturn actionDelegateViewFlow
        component = BcmcComponent(bcmcDelegate, genericActionDelegate, actionHandlingComponent)

        component.viewFlow.test {
            // this value should match the value of the main delegate and not the action delegate
            // and in practice the initial value of the action delegate view flow is always null so it should be ignored
            assertEquals(BcmcComponentViewType, awaitItem())

            actionDelegateViewFlow.emit(TestComponentViewType.VIEW_TYPE_2)
            assertEquals(TestComponentViewType.VIEW_TYPE_2, awaitItem())

            expectNoEvents()
        }
    }

    @Test
    fun `when isConfirmationRequired, then delegate is called`() {
        component.isConfirmationRequired()
        verify(bcmcDelegate).isConfirmationRequired()
    }

    @Test
    fun `when submit is called and active delegate is the payment delegate, then delegate onSubmit is called`() {
        whenever(component.delegate).thenReturn(bcmcDelegate)
        component.submit()
        verify(bcmcDelegate).onSubmit()
    }

    @Test
    fun `when submit is called and active delegate is the action delegate, then delegate onSubmit is not called`() {
        whenever(component.delegate).thenReturn(genericActionDelegate)
        component.submit()
        verify(bcmcDelegate, never()).onSubmit()
    }
}
