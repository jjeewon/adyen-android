/*
 * Copyright (c) 2020 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 1/12/2020.
 */

package com.adyen.checkout.dropin.ui.stored

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.adyen.checkout.components.ButtonComponent
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.PaymentComponent
import com.adyen.checkout.components.PaymentComponentEvent
import com.adyen.checkout.components.image.loadLogo
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.components.ui.PaymentComponentUIState
import com.adyen.checkout.components.ui.UIStateDelegate
import com.adyen.checkout.components.util.CurrencyUtils
import com.adyen.checkout.components.util.DateUtils
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.exception.ComponentException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.R
import com.adyen.checkout.dropin.databinding.FragmentStoredPaymentMethodBinding
import com.adyen.checkout.dropin.getComponentFor
import com.adyen.checkout.dropin.ui.base.DropInBottomSheetDialogFragment
import com.adyen.checkout.dropin.ui.paymentmethods.GenericStoredModel
import com.adyen.checkout.dropin.ui.paymentmethods.StoredCardModel
import com.adyen.checkout.dropin.ui.viewModelsFactory
import com.adyen.checkout.dropin.ui.viewmodel.PreselectedStoredPaymentViewModel
import com.adyen.checkout.dropin.ui.viewmodel.PreselectedStoredState
import com.adyen.checkout.dropin.ui.viewmodel.PreselectedStoredState.ShowStoredPaymentDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private val TAG = LogUtil.getTag()
private const val STORED_PAYMENT_KEY = "STORED_PAYMENT"

@Suppress("TooManyFunctions")
internal class PreselectedStoredPaymentMethodFragment : DropInBottomSheetDialogFragment() {

    private val storedPaymentViewModel: PreselectedStoredPaymentViewModel by viewModelsFactory {
        PreselectedStoredPaymentViewModel(
            storedPaymentMethod,
            component.requiresInput(),
            dropInViewModel.dropInConfiguration
        )
    }

    private var _binding: FragmentStoredPaymentMethodBinding? = null
    private val binding: FragmentStoredPaymentMethodBinding get() = requireNotNull(_binding)
    private lateinit var storedPaymentMethod: StoredPaymentMethod
    private lateinit var component: PaymentComponent<*>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        storedPaymentMethod = arguments?.getParcelable(STORED_PAYMENT_KEY) ?: StoredPaymentMethod()

        if (storedPaymentMethod.type.isNullOrEmpty()) {
            throw ComponentException("Stored payment method is empty or not found.")
        }

        component =
            getComponentFor(this, storedPaymentMethod, dropInViewModel.dropInConfiguration, dropInViewModel.amount)
        component.observe(viewLifecycleOwner, ::onPaymentComponentEvent)

        _binding = FragmentStoredPaymentMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun observeUIState() {
        val uiStateDelegate = component.delegate as? UIStateDelegate

        if (uiStateDelegate == null) {
            Log.e(TAG, "Delegate is not type of UIStateDelegate")
            return
        }

        uiStateDelegate.uiStateFlow
            .onEach {
                setPaymentPendingInitialization(it is PaymentComponentUIState.Loading)
            }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun onPaymentComponentEvent(event: PaymentComponentEvent<*>) {
        when (event) {
            is PaymentComponentEvent.StateChanged -> {
                // no ops
            }
            is PaymentComponentEvent.Error -> handleError(event.error)
            is PaymentComponentEvent.ActionDetails -> {
                throw IllegalStateException("This event should not be used in drop-in")
            }
            is PaymentComponentEvent.Submit -> protocol.requestPaymentsCall(event.state)
        }
    }

    @SuppressLint("ResourceAsColor")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d(TAG, "onViewCreated")
        binding.paymentMethodsListHeader.paymentMethodHeaderTitle.setText(R.string.store_payment_methods_header)
        binding.storedPaymentMethodItem.root.setBackgroundColor(android.R.color.transparent)
        observeState()
        observe()
        observeUIState()

        if (component.requiresInput()) {
            binding.payButton.setText(R.string.continue_button)
        } else {
            val value = CurrencyUtils.formatAmount(
                dropInViewModel.amount,
                dropInViewModel.dropInConfiguration.shopperLocale
            )
            binding.payButton.text = getString(R.string.pay_button_with_value, value)
        }

        if (dropInViewModel.dropInConfiguration.isRemovingStoredPaymentMethodsEnabled) {
            binding.storedPaymentMethodItem.paymentMethodItemUnderlayButton.setOnClickListener {
                showRemoveStoredPaymentDialog()
            }
        }

        binding.payButton.setOnClickListener {
            storedPaymentViewModel.payButtonClicked()
        }

        binding.changePaymentMethodButton.setOnClickListener {
            protocol.showPaymentMethodsDialog()
        }
    }

    private fun observeState() {
        storedPaymentViewModel.componentFragmentState.onEach {
            Logger.v(TAG, "state: $it")
            when (it) {
                is ShowStoredPaymentDialog -> protocol.showStoredComponentDialog(storedPaymentMethod, true)
                is PreselectedStoredState.Submit -> {
                    (component as? ButtonComponent)?.submit()
                        ?: throw CheckoutException("Component must be of type ButtonComponent.")
                }
                else -> { // do nothing
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setPaymentPendingInitialization(pending: Boolean) {
        binding.payButton.isVisible = !pending
        if (pending) binding.progressBar.show() else binding.progressBar.hide()
    }

    private fun handleError(componentError: ComponentError) {
        Logger.e(TAG, componentError.errorMessage)
        protocol.showError(getString(R.string.component_error), componentError.errorMessage, true)
    }

    private fun observe() {
        storedPaymentViewModel.storedPaymentLiveData.onEach {
            binding.storedPaymentMethodItem.swipeToRevealLayout.setDragLocked(!it.isRemovable)
            when (it) {
                is StoredCardModel -> {
                    binding.storedPaymentMethodItem.textViewTitle.text =
                        requireActivity().getString(R.string.card_number_4digit, it.lastFour)
                    binding.storedPaymentMethodItem.imageViewLogo.loadLogo(
                        environment = dropInViewModel.dropInConfiguration.environment,
                        txVariant = it.imageId,
                    )
                    binding.storedPaymentMethodItem.textViewDetail.text =
                        DateUtils.parseDateToView(it.expiryMonth, it.expiryYear)
                    binding.storedPaymentMethodItem.textViewDetail.isVisible = true
                }
                is GenericStoredModel -> {
                    binding.storedPaymentMethodItem.textViewTitle.text = it.name
                    binding.storedPaymentMethodItem.textViewDetail.isVisible = false
                    binding.storedPaymentMethodItem.imageViewLogo.loadLogo(
                        environment = dropInViewModel.dropInConfiguration.environment,
                        txVariant = it.imageId,
                    )
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun showRemoveStoredPaymentDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.checkout_giftcard_remove_gift_cards_title)
            .setMessage(R.string.checkout_remove_stored_payment_method_body)
            .setPositiveButton(R.string.checkout_giftcard_remove_gift_cards_positive_button) { dialog, _ ->
                val storedPaymentMethod = StoredPaymentMethod().apply {
                    id = storedPaymentMethod.id
                }
                protocol.removeStoredPaymentMethod(storedPaymentMethod)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.checkout_giftcard_remove_gift_cards_negative_button) { dialog, _ ->
                binding.storedPaymentMethodItem.root.collapseUnderlay()
                dialog.dismiss()
            }
            .show()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Logger.d(TAG, "onCancel")
        protocol.terminateDropIn()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(storedPaymentMethod: StoredPaymentMethod) =
            PreselectedStoredPaymentMethodFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(STORED_PAYMENT_KEY, storedPaymentMethod)
                }
            }
    }
}
