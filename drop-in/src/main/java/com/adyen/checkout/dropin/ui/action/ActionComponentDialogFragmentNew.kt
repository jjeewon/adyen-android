/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by josephj on 31/8/2022.
 */

package com.adyen.checkout.dropin.ui.action

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.adyen.checkout.action.GenericActionComponent
import com.adyen.checkout.action.GenericActionConfiguration
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.components.ui.view.AdyenComponentView
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.R
import com.adyen.checkout.dropin.databinding.FragmentGenericActionComponentBinding
import com.adyen.checkout.dropin.getActionProviderFor
import com.adyen.checkout.dropin.ui.base.DropInBottomSheetDialogFragment

@SuppressWarnings("TooManyFunctions")
class ActionComponentDialogFragmentNew : DropInBottomSheetDialogFragment(), Observer<ActionComponentData> {

    companion object {
        private val TAG = LogUtil.getTag()

        const val ACTION = "ACTION"
        const val ACTION_CONFIGURATION = "ACTION_CONFIGURATION"

        fun newInstance(
            action: Action,
            actionConfiguration: GenericActionConfiguration
        ): ActionComponentDialogFragmentNew {
            val args = Bundle()
            args.putParcelable(ACTION, action)
            args.putParcelable(ACTION_CONFIGURATION, actionConfiguration)

            val componentDialogFragment = ActionComponentDialogFragmentNew()
            componentDialogFragment.arguments = args

            return componentDialogFragment
        }
    }

    private var _binding: FragmentGenericActionComponentBinding? = null
    private val binding: FragmentGenericActionComponentBinding get() = requireNotNull(_binding)
    private lateinit var action: Action
    private lateinit var actionType: String
    private lateinit var actionConfiguration: GenericActionConfiguration
    private lateinit var actionComponent: GenericActionComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d(TAG, "onCreate")
        action = arguments?.getParcelable(ACTION) ?: throw IllegalArgumentException("Action not found")
        actionType = action.type ?: throw IllegalArgumentException("Action type not found")
        actionConfiguration =
            arguments?.getParcelable(ACTION_CONFIGURATION) ?: throw IllegalArgumentException("Configuration not found")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericActionComponentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d(TAG, "onViewCreated")
        binding.header.isVisible = false

        try {

            actionComponent =
                GenericActionComponent.PROVIDER.get(this, requireActivity().application, actionConfiguration)

            if (shouldFinishWithAction()) {
                with(binding.buttonFinish) {
                    isVisible = true
                    setOnClickListener { protocol.finishWithAction() }
                }
            }

            actionComponent.handleAction(requireActivity(), action)
            attachComponent(actionComponent, binding.componentView)

        } catch (e: CheckoutException) {
            handleError(ComponentError(e))
        }
    }

    override fun onBackPressed(): Boolean {
        // polling will be canceled by lifecycle event
        when {
            shouldFinishWithAction() -> {
                protocol.finishWithAction()
            }
            dropInViewModel.shouldSkipToSinglePaymentMethod() -> {
                protocol.terminateDropIn()
            }
            else -> {
                protocol.showPaymentMethodsDialog()
            }
        }
        return true
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Logger.d(TAG, "onCancel")
        if (shouldFinishWithAction()) {
            protocol.finishWithAction()
        } else {
            protocol.terminateDropIn()
        }
    }

    override fun onChanged(actionComponentData: ActionComponentData?) {
        Logger.d(TAG, "onChanged")
        if (actionComponentData != null) {
            protocol.requestDetailsCall(actionComponentData)
        }
    }

    private fun attachComponent(
        component: GenericActionComponent,
        componentView: AdyenComponentView
    ) {
        componentView.attach(component, viewLifecycleOwner)
        component.observe(viewLifecycleOwner, this)
        component.observeErrors(viewLifecycleOwner, createErrorHandlerObserver())
    }

    private fun createErrorHandlerObserver(): Observer<ComponentError> {
        return Observer {
            if (it != null) {
                handleError(it)
            }
        }
    }

    private fun handleError(componentError: ComponentError) {
        Logger.e(TAG, componentError.errorMessage)
        protocol.showError(getString(R.string.action_failed), componentError.errorMessage, true)
    }

    // TODO this should rely on the generic component not the individual providers
    private fun shouldFinishWithAction(): Boolean {
        return getActionProviderFor(action)?.providesDetails() == false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
