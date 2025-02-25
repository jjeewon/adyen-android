/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 24/11/2022.
 */

package com.adyen.checkout.components.analytics

import androidx.annotation.RestrictTo
import com.adyen.checkout.components.api.AnalyticsService
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.core.util.runSuspendCatching
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAnalyticsRepository(
    private val packageName: String,
    private val locale: Locale,
    private val source: AnalyticsSource,
    private val analyticsService: AnalyticsService,
    private val analyticsMapper: AnalyticsMapper,
) : AnalyticsRepository {

    override suspend fun sendAnalyticsEvent() {
        runSuspendCatching {
            val queryParameters = analyticsMapper.getQueryParameters(packageName, locale, source)
            analyticsService.sendEvent(queryParameters)
            Logger.v(TAG, "Analytics event sent")
        }
            .onFailure { e -> Logger.e(TAG, "Failed to send analytics event", e) }
    }

    companion object {
        private val TAG = LogUtil.getTag()
    }
}
