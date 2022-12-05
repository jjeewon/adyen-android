/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by atef on 5/12/2022.
 */

package com.adyen.checkout.components.imageloader.repository

import android.graphics.Bitmap

interface ImageLoaderRepository {
    suspend fun load(
        imagePath: String
    ): Bitmap?

    fun clearCache()
}
