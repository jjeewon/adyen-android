package com.adyen.checkout.example.ui.main

import androidx.annotation.StringRes
import com.adyen.checkout.example.R

internal sealed class ComponentItem {

    abstract val stringResource: Int

    data class Title(@StringRes override val stringResource: Int) : ComponentItem()

    sealed class Entry(@StringRes override val stringResource: Int) : ComponentItem() {
        object DropIn : Entry(R.string.drop_in_entry)
        object DropInWithSession : Entry(R.string.drop_in_with_session_entry)
        object DropInWithCustomSession : Entry(R.string.drop_in_with_session_custom_entry)
        object Bacs : Entry(R.string.bacs_component_entry)
        object Blik : Entry(R.string.blik_component_entry)
        object Card : Entry(R.string.card_component_entry)
        object Instant : Entry(R.string.instant_component_entry)
        object CardWithSession : Entry(R.string.card_component_with_session_entry)
    }
}
