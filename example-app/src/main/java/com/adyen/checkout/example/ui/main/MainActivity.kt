/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/10/2019.
 */

package com.adyen.checkout.example.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInCallback
import com.adyen.checkout.dropin.DropInResult
import com.adyen.checkout.example.R
import com.adyen.checkout.example.databinding.ActivityMainBinding
import com.adyen.checkout.example.service.ExampleFullAsyncDropInService
import com.adyen.checkout.example.service.ExampleSessionsDropInService
import com.adyen.checkout.example.ui.bacs.BacsFragment
import com.adyen.checkout.example.ui.blik.BlikActivity
import com.adyen.checkout.example.ui.card.CardActivity
import com.adyen.checkout.example.ui.card.SessionsCardActivity
import com.adyen.checkout.example.ui.configuration.ConfigurationActivity
import com.adyen.checkout.example.ui.instant.InstantFragment
import com.adyen.checkout.redirect.RedirectComponent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), DropInCallback {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val dropInLauncher = DropIn.registerForDropInResult(this, this)

    private var componentItemAdapter: ComponentItemAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Insert return url in extras, so we can access it in the ViewModel through SavedStateHandle
        intent = (intent ?: Intent()).putExtra(RETURN_URL_EXTRA, RedirectComponent.getReturnUrl(applicationContext))

        Logger.d(TAG, "onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        componentItemAdapter = ComponentItemAdapter(
            viewModel::onComponentEntryClick
        )
        binding.componentList.adapter = componentItemAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.viewState.collect(::onViewState) }
                launch { viewModel.eventFlow.collect(::onMainEvent) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Logger.d(TAG, "onOptionsItemSelected")
        if (item.itemId == R.id.settings) {
            val intent = Intent(this@MainActivity, ConfigurationActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDropInResult(dropInResult: DropInResult?) = viewModel.onDropInResult(dropInResult)

    private fun onViewState(viewState: MainViewState) {
        when (viewState) {
            MainViewState.Loading -> setLoading(true)
            is MainViewState.Result -> {
                setLoading(false)
                componentItemAdapter?.items = viewState.items
            }
        }
    }

    private fun onMainEvent(event: MainEvent) {
        when (event) {
            is MainEvent.NavigateTo -> onNavigateTo(event.destination)
            is MainEvent.Toast -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onNavigateTo(navigation: MainNavigation) {
        when (navigation) {
            is MainNavigation.DropIn -> {
                DropIn.startPayment(
                    this,
                    dropInLauncher,
                    navigation.paymentMethodsApiResponse,
                    navigation.dropInConfiguration,
                    ExampleFullAsyncDropInService::class.java,
                )
            }
            is MainNavigation.DropInWithSession -> {
                DropIn.startPaymentWithSession(
                    this,
                    dropInLauncher,
                    navigation.checkoutSession,
                    navigation.dropInConfiguration,
                )
            }
            is MainNavigation.DropInWithCustomSession -> {
                DropIn.startPaymentWithSession(
                    this,
                    dropInLauncher,
                    navigation.checkoutSession,
                    navigation.dropInConfiguration,
                    ExampleSessionsDropInService::class.java
                )
            }
            is MainNavigation.Bacs -> BacsFragment.show(supportFragmentManager)
            is MainNavigation.Blik -> {
                val intent = Intent(this, BlikActivity::class.java)
                startActivity(intent)
            }
            MainNavigation.Card -> {
                val intent = Intent(this, CardActivity::class.java)
                startActivity(intent)
            }
            MainNavigation.CardWithSession -> {
                val intent = Intent(this, SessionsCardActivity::class.java)
                startActivity(intent)
            }
            is MainNavigation.Instant -> {
                InstantFragment.show(supportFragmentManager)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressIndicator.show()
        } else {
            binding.progressIndicator.hide()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        componentItemAdapter = null
    }

    companion object {
        private val TAG = LogUtil.getTag()

        internal const val RETURN_URL_EXTRA = "RETURN_URL_EXTRA"
    }
}
