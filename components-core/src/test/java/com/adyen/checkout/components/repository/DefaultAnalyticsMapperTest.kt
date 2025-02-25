/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 25/11/2022.
 */

package com.adyen.checkout.components.repository

import com.adyen.checkout.components.analytics.AnalyticsMapper
import com.adyen.checkout.components.analytics.AnalyticsSource
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

internal class DefaultAnalyticsMapperTest {

    private val analyticsMapper: AnalyticsMapper = AnalyticsMapper()

    @Nested
    @DisplayName("when getFlavorQueryParameter is called and")
    inner class GetFlavorQueryParameterTest {

        @Test
        fun `source is drop-in then returned value is dropin`() {
            val actual = analyticsMapper.getFlavorQueryParameter(AnalyticsSource.DropIn())
            assertEquals("dropin", actual)
        }

        @Test
        fun `source is a component created from drop-in then returned value is dropin`() {
            val actual = analyticsMapper.getFlavorQueryParameter(
                AnalyticsSource.PaymentComponent(
                    isCreatedByDropIn = true,
                    paymentMethod = PaymentMethod(),
                )
            )
            assertEquals("dropin", actual)
        }

        @Test
        fun `source is a component not created from drop-in then returned value is components`() {
            val actual = analyticsMapper.getFlavorQueryParameter(
                AnalyticsSource.PaymentComponent(
                    isCreatedByDropIn = false,
                    paymentMethod = PaymentMethod(),
                )
            )
            assertEquals("components", actual)
        }
    }

    @Nested
    @DisplayName("when getComponentQueryParameter is called and")
    inner class GetComponentQueryParameterTest {

        @Test
        fun `source is drop-in then returned value is dropin`() {
            val actual = analyticsMapper.getComponentQueryParameter(AnalyticsSource.DropIn())
            assertEquals("dropin", actual)
        }

        @Test
        fun `source is a component with a payment method then returned value is the payment method type`() {
            val actual = analyticsMapper.getComponentQueryParameter(
                AnalyticsSource.PaymentComponent(
                    isCreatedByDropIn = true,
                    paymentMethod = PaymentMethod(type = "PAYMENT_METHOD_TYPE"),
                )
            )
            assertEquals("PAYMENT_METHOD_TYPE", actual)
        }

        @Test
        fun `source is a component with a stored payment method then returned value is the stored payment method type`() {
            val actual = analyticsMapper.getComponentQueryParameter(
                AnalyticsSource.PaymentComponent(
                    isCreatedByDropIn = false,
                    storedPaymentMethod = StoredPaymentMethod(type = "STORED_PAYMENT_METHOD_TYPE"),
                )
            )
            assertEquals("STORED_PAYMENT_METHOD_TYPE", actual)
        }
    }

    @Nested
    @DisplayName("when getQueryParameters is called")
    inner class GetQueryParametersTest {

        @Test
        fun `then returned values should match expected`() {
            val actual = analyticsMapper.getQueryParameters(
                packageName = "PACKAGE_NAME",
                locale = Locale("en", "US"),
                source = AnalyticsSource.PaymentComponent(
                    isCreatedByDropIn = false,
                    PaymentMethod(type = "PAYMENT_METHOD_TYPE")
                )
            )

            val expected = mapOf(
                "payload_version" to "1",
                "version" to "4.9.1",
                "flavor" to "components",
                "component" to "PAYMENT_METHOD_TYPE",
                "locale" to "en_US",
                "platform" to "android",
                "referer" to "PACKAGE_NAME",
                "device_brand" to "null",
                "device_model" to "null",
                "system_version" to "0",
            )

            assertEquals(expected.toString(), actual.toString())
        }
    }
}
