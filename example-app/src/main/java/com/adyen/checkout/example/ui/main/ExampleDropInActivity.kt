/*
 * Copyright (c) 2022 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by onurk on 30/11/2022.
 */

package com.adyen.checkout.example.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInCallback
import com.adyen.checkout.dropin.DropInResult
import com.adyen.checkout.example.databinding.ActivityExampleDropinBinding
import com.adyen.checkout.example.service.MyAsyncDropInService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExampleDropInActivity : AppCompatActivity(), DropInCallback {

    private lateinit var binding: ActivityExampleDropinBinding
    private val viewModel: DropInViewModel by viewModels()
    private val dropInLauncher = DropIn.registerForDropInResult(this, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExampleDropinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.viewState.collect(::onViewStateChange) }
                launch { viewModel.event.collect(::onEventChange) }
            }
        }
        binding.dropIn.setOnClickListener { viewModel.dropInClick() }
    }

    private fun onEventChange(navigation: DropInNavigation) {
        when (navigation) {
            is DropInNavigation.DropIn -> {
                DropIn.startPayment(
                    this,
                    dropInLauncher,
                    navigation.paymentMethodsApiResponse,
                    navigation.dropInConfiguration,
                    MyAsyncDropInService::class.java,
                )
            }
        }
    }

    private fun onViewStateChange(viewState: DropInViewState) {
        when (viewState) {
            is DropInViewState.Error -> {
                Toast.makeText(this, viewState.message, Toast.LENGTH_SHORT).show()
            }
            DropInViewState.Loading -> {
                binding.progressIndicator.isVisible = true
                binding.dropIn.isVisible = false
            }
            DropInViewState.Success -> {
                binding.progressIndicator.isVisible = false
                binding.dropIn.isVisible = true
            }
        }
    }

    override fun onDropInResult(dropInResult: DropInResult?) {
        if (dropInResult == null) return
        when (dropInResult) {
            is DropInResult.CancelledByUser -> Toast.makeText(this, "Canceled by user", Toast.LENGTH_SHORT).show()
            is DropInResult.Error -> Toast.makeText(this, dropInResult.reason, Toast.LENGTH_SHORT).show()
            is DropInResult.Finished -> Toast.makeText(this, dropInResult.result, Toast.LENGTH_SHORT).show()
        }
    }
}
