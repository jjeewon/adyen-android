/*
 * Copyright (c) 2020 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 11/8/2020.
 */

package com.adyen.checkout.mbway;

import androidx.annotation.NonNull;

import com.adyen.checkout.components.base.InputData;

@SuppressWarnings("AbbreviationAsWordInName")
class MBWayInputData implements InputData {

    private String mMobilePhoneNumber = "";

    @NonNull
    public String getMobilePhoneNumber() {
        return mMobilePhoneNumber;
    }

    public void setMobilePhoneNumber(@NonNull String mobilePhoneNumber) {
        mMobilePhoneNumber = mobilePhoneNumber;
    }
}
