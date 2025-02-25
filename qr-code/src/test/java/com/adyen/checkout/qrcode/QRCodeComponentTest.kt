/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 19/12/2022.
 */

package com.adyen.checkout.qrcode

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.adyen.checkout.components.ActionComponentEvent
import com.adyen.checkout.components.model.payments.response.QrCodeAction
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class, TestDispatcherExtension::class)
internal class QRCodeComponentTest(
    @Mock private val qrCodeDelegate: QRCodeDelegate,
) {

    private lateinit var component: QRCodeComponent

    @BeforeEach
    fun before() {
        Logger.setLogcatLevel(Logger.NONE)

        whenever(qrCodeDelegate.viewFlow) doReturn MutableStateFlow(QrCodeComponentViewType.QR_CODE)
        component = QRCodeComponent(qrCodeDelegate)
    }

    @Test
    fun `when component is created then delegate is initialized`() {
        verify(qrCodeDelegate).initialize(component.viewModelScope)
    }

    @Test
    fun `when component is cleared then delegate is cleared`() {
        component.invokeOnCleared()

        verify(qrCodeDelegate).onCleared()
    }

    @Test
    fun `when observe is called then observe in delegate is called`() {
        val lifecycleOwner = mock<LifecycleOwner>()
        val callback: (ActionComponentEvent) -> Unit = {}

        component.observe(lifecycleOwner, callback)

        verify(qrCodeDelegate).observe(lifecycleOwner, component.viewModelScope, callback)
    }

    @Test
    fun `when removeObserver is called then removeObserver in delegate is called`() {
        component.removeObserver()

        verify(qrCodeDelegate).removeObserver()
    }

    @Test
    fun `when component is initialized then view flow should match delegate view flow`() = runTest {
        component.viewFlow.test {
            assertEquals(QrCodeComponentViewType.QR_CODE, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `when delegate view flow emits a value then component view flow should match that value`() = runTest {
        val delegateViewFlow = MutableStateFlow(QrCodeComponentViewType.QR_CODE)
        whenever(qrCodeDelegate.viewFlow) doReturn delegateViewFlow
        component = QRCodeComponent(qrCodeDelegate)

        component.viewFlow.test {
            assertEquals(QrCodeComponentViewType.QR_CODE, awaitItem())

            delegateViewFlow.emit(QrCodeComponentViewType.REDIRECT)
            assertEquals(QrCodeComponentViewType.REDIRECT, awaitItem())

            expectNoEvents()
        }
    }

    @Test
    fun `when handleAction is called then handleAction in delegate is called`() {
        val action = QrCodeAction()
        val activity = Activity()
        component.handleAction(action, activity)

        verify(qrCodeDelegate).handleAction(action, activity)
    }

    @Test
    fun `when handleIntent is called then handleIntent in delegate is called`() {
        val intent = Intent()
        component.handleIntent(intent)

        verify(qrCodeDelegate).handleIntent(intent)
    }
}
