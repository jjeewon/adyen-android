/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 1/9/2022.
 */

package com.adyen.checkout.components.base

import kotlinx.coroutines.CoroutineScope

interface ComponentDelegate {
    val componentParams: ComponentParams

    /**
     * Use this method if your delegate needs to use a [CoroutineScope].
     *
     * Do not keep a local references of this scope if you don't need to.
     *
     * If you have to keep any references to [CoroutineScope], use [onCleared] to clear them.
     */
    fun initialize(coroutineScope: CoroutineScope)

    fun onCleared()
}
