/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 5/4/2019.
 */
package com.adyen.checkout.redirect

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.checkout.components.ActionComponent
import com.adyen.checkout.components.ActionComponentEvent
import com.adyen.checkout.components.ActionComponentProvider
import com.adyen.checkout.components.base.IntentHandlingComponent
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.components.ui.ViewableComponent
import com.adyen.checkout.components.ui.view.ComponentViewType
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import kotlinx.coroutines.flow.Flow

class RedirectComponent internal constructor(
    override val delegate: RedirectDelegate
) : ViewModel(),
    ActionComponent,
    IntentHandlingComponent,
    ViewableComponent {

    override val viewFlow: Flow<ComponentViewType?> = delegate.viewFlow

    init {
        delegate.initialize(viewModelScope)
    }

    override fun observe(lifecycleOwner: LifecycleOwner, callback: (ActionComponentEvent) -> Unit) {
        delegate.observe(lifecycleOwner, viewModelScope, callback)
    }

    override fun removeObserver() {
        delegate.removeObserver()
    }

    override fun canHandleAction(action: Action): Boolean {
        return PROVIDER.canHandleAction(action)
    }

    override fun handleAction(action: Action, activity: Activity) {
        delegate.handleAction(action, activity)
    }

    /**
     * Call this method when receiving the return URL from the redirect with the result data.
     * This result will be in the [Intent.getData] and begins with the returnUrl you specified on the payments/ call.
     *
     * @param intent The received [Intent].
     */
    override fun handleIntent(intent: Intent) {
        delegate.handleIntent(intent)
    }

    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "onCleared")
        delegate.onCleared()
    }

    companion object {
        private val TAG = LogUtil.getTag()

        @JvmField
        val PROVIDER: ActionComponentProvider<RedirectComponent, RedirectConfiguration, RedirectDelegate> =
            RedirectComponentProvider()

        /**
         * The suggested scheme to be used in the intent filter to receive the redirect result.
         * This value should be the beginning of the `returnUrl` sent on the payments/ call.
         */
        const val REDIRECT_RESULT_SCHEME = BuildConfig.checkoutRedirectScheme + "://"

        /**
         * Returns the suggested value to be used as the `returnUrl` value in the payments/ call.
         *
         * @param context The context provides the package name which constitutes part of the ReturnUrl
         * @return The suggested `returnUrl` to be used. Consists of [REDIRECT_RESULT_SCHEME] + App package name.
         */
        fun getReturnUrl(context: Context): String {
            return REDIRECT_RESULT_SCHEME + context.packageName
        }
    }
}
