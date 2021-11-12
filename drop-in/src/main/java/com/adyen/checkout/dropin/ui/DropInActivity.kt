/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 9/4/2019.
 */

package com.adyen.checkout.dropin.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.analytics.AnalyticEvent
import com.adyen.checkout.components.analytics.AnalyticsDispatcher
import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.components.model.payments.response.BalanceResult
import com.adyen.checkout.components.model.payments.response.OrderResponse
import com.adyen.checkout.components.util.PaymentMethodTypes
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.ActionHandler
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.DropInPrefs
import com.adyen.checkout.dropin.R
import com.adyen.checkout.dropin.service.BalanceDropInServiceResult
import com.adyen.checkout.dropin.service.BaseDropInServiceResult
import com.adyen.checkout.dropin.service.DropInService
import com.adyen.checkout.dropin.service.DropInServiceInterface
import com.adyen.checkout.dropin.service.DropInServiceResult
import com.adyen.checkout.dropin.service.DropInServiceResultError
import com.adyen.checkout.dropin.service.OrderDropInServiceResult
import com.adyen.checkout.dropin.ui.action.ActionComponentDialogFragment
import com.adyen.checkout.dropin.ui.base.DropInBottomSheetDialogFragment
import com.adyen.checkout.dropin.ui.component.CardComponentDialogFragment
import com.adyen.checkout.dropin.ui.component.GenericComponentDialogFragment
import com.adyen.checkout.dropin.ui.component.GiftCardComponentDialogFragment
import com.adyen.checkout.dropin.ui.giftcard.GiftCardBalanceResult
import com.adyen.checkout.dropin.ui.giftcard.GiftCardPaymentConfirmationData
import com.adyen.checkout.dropin.ui.giftcard.GiftCardPaymentConfirmationDialogFragment
import com.adyen.checkout.dropin.ui.paymentmethods.PaymentMethodListDialogFragment
import com.adyen.checkout.dropin.ui.stored.PreselectedStoredPaymentMethodFragment
import com.adyen.checkout.giftcard.GiftCardComponentState
import com.adyen.checkout.googlepay.GooglePayComponent
import com.adyen.checkout.googlepay.GooglePayComponentState
import com.adyen.checkout.googlepay.GooglePayConfiguration
import com.adyen.checkout.redirect.RedirectUtil
import com.adyen.checkout.wechatpay.WeChatPayUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi

private val TAG = LogUtil.getTag()

private const val PRESELECTED_PAYMENT_METHOD_FRAGMENT_TAG = "PRESELECTED_PAYMENT_METHOD_FRAGMENT"
private const val PAYMENT_METHODS_LIST_FRAGMENT_TAG = "PAYMENT_METHODS_LIST_FRAGMENT"
private const val COMPONENT_FRAGMENT_TAG = "COMPONENT_DIALOG_FRAGMENT"
private const val ACTION_FRAGMENT_TAG = "ACTION_DIALOG_FRAGMENT"
private const val LOADING_FRAGMENT_TAG = "LOADING_DIALOG_FRAGMENT"
private const val GIFT_CARD_PAYMENT_CONFIRMATION_FRAGMENT_TAG = "GIFT_CARD_PAYMENT_CONFIRMATION_FRAGMENT"

private const val GOOGLE_PAY_REQUEST_CODE = 1

/**
 * Activity that presents the available PaymentMethods to the Shopper.
 */
@Suppress("TooManyFunctions")
class DropInActivity : AppCompatActivity(), DropInBottomSheetDialogFragment.Protocol, ActionHandler.ActionHandlingInterface {

    private val dropInViewModel: DropInViewModel by viewModels()

    private lateinit var googlePayComponent: GooglePayComponent

    private lateinit var actionHandler: ActionHandler

    private val loadingDialog = LoadingDialogFragment.newInstance()

    private val googlePayObserver: Observer<GooglePayComponentState> = Observer {
        if (it?.isValid == true) {
            requestPaymentsCall(it)
        }
    }

    private val googlePayErrorObserver: Observer<ComponentError> = Observer {
        Logger.d(TAG, "GooglePay error - ${it?.errorMessage}")
        showPaymentMethodsDialog()
    }

    private var dropInService: DropInServiceInterface? = null
    private var serviceBound: Boolean = false

    // these queues exist for when a call is requested before the service is bound
    private var paymentDataQueue: PaymentComponentState<*>? = null
    private var actionDataQueue: ActionComponentData? = null
    private var balanceDataQueue: GiftCardComponentState? = null
    private var orderDataQueue: Unit? = null

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            Logger.d(TAG, "onServiceConnected")
            val dropInBinder = binder as? DropInService.DropInBinder ?: return
            dropInService = dropInBinder.getService()
            dropInService?.observeResult(this@DropInActivity) { handleDropInServiceResult(it) }

            paymentDataQueue?.let {
                Logger.d(TAG, "Sending queued payment request")
                requestPaymentsCall(it)
                paymentDataQueue = null
            }

            actionDataQueue?.let {
                Logger.d(TAG, "Sending queued action request")
                requestDetailsCall(it)
                actionDataQueue = null
            }
            balanceDataQueue?.let {
                Logger.d(TAG, "Sending queued action request")
                requestBalanceCall(it)
                balanceDataQueue = null
            }
            orderDataQueue?.let {
                Logger.d(TAG, "Sending queued order request")
                requestOrdersCall()
                orderDataQueue = null
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Logger.d(TAG, "onServiceDisconnected")
            dropInService = null
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        Logger.d(TAG, "attachBaseContext")
        super.attachBaseContext(createLocalizedContext(newBase))
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d(TAG, "onCreate - $savedInstanceState")
        setContentView(R.layout.activity_drop_in)
        overridePendingTransition(0, 0)

        val bundle = savedInstanceState ?: intent.extras

        val initializationSuccessful = assertBundleExists(bundle)
        if (!initializationSuccessful) {
            terminateWithError("Initialization failed")
            return
        }

        if (noDialogPresent()) {
            when {
                dropInViewModel.shouldSkipToSinglePaymentMethod() -> {
                    val firstPaymentMethod = dropInViewModel.paymentMethodsApiResponse.paymentMethods?.firstOrNull()
                    if (firstPaymentMethod != null) {
                        showComponentDialog(firstPaymentMethod)
                    } else {
                        throw CheckoutException("First payment method is null")
                    }
                }
                dropInViewModel.showPreselectedStored -> showPreselectedDialog()
                else -> showPaymentMethodsDialog()
            }
        }

        actionHandler = ActionHandler(this, dropInViewModel.dropInConfiguration)
        actionHandler.restoreState(this, savedInstanceState)

        handleIntent(intent)

        sendAnalyticsEvent()
    }

    private fun noDialogPresent(): Boolean {
        return getFragmentByTag(PRESELECTED_PAYMENT_METHOD_FRAGMENT_TAG) == null &&
            getFragmentByTag(PAYMENT_METHODS_LIST_FRAGMENT_TAG) == null &&
            getFragmentByTag(COMPONENT_FRAGMENT_TAG) == null &&
            getFragmentByTag(ACTION_FRAGMENT_TAG) == null &&
            getFragmentByTag(GIFT_CARD_PAYMENT_CONFIRMATION_FRAGMENT_TAG) == null
    }

    private fun createLocalizedContext(baseContext: Context?): Context? {
        if (baseContext == null) return baseContext

        // We need to get the Locale from sharedPrefs because attachBaseContext is called before onCreate, so we don't have the Config object yet.
        val locale = DropInPrefs.getShopperLocale(baseContext)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        return baseContext.createConfigurationContext(config)
    }

    private fun assertBundleExists(bundle: Bundle?): Boolean {
        if (bundle == null) {
            Logger.e(TAG, "Failed to initialize - bundle is null")
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GOOGLE_PAY_REQUEST_CODE -> googlePayComponent.handleActivityResult(resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Logger.d(TAG, "onNewIntent")
        if (intent != null) {
            handleIntent(intent)
        } else {
            Logger.e(TAG, "Null intent")
        }
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    private fun bindService() {
        val bound = DropInService.bindService(this, serviceConnection, dropInViewModel.dropInConfiguration.serviceComponentName)
        if (bound) {
            serviceBound = true
        } else {
            Logger.e(
                TAG,
                "Error binding to ${dropInViewModel.dropInConfiguration.serviceComponentName.className}. " +
                    "The system couldn't find the service or your client doesn't have permission to bind to it"
            )
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService()
    }

    private fun unbindService() {
        if (serviceBound) {
            DropInService.unbindService(this, serviceConnection)
            serviceBound = false
        }
    }

    override fun requestPaymentsCall(paymentComponentState: PaymentComponentState<*>) {
        Logger.d(TAG, "requestPaymentsCall")
        if (dropInService == null) {
            Logger.e(TAG, "service is disconnected, adding to queue")
            paymentDataQueue = paymentComponentState
            return
        }
        dropInViewModel.isWaitingResult = true
        setLoading(true)
        dropInViewModel.updatePaymentComponentStateForPaymentsCall(paymentComponentState)
        dropInService?.requestPaymentsCall(paymentComponentState)
    }

    override fun requestDetailsCall(actionComponentData: ActionComponentData) {
        Logger.d(TAG, "requestDetailsCall")
        if (dropInService == null) {
            Logger.e(TAG, "service is disconnected, adding to queue")
            actionDataQueue = actionComponentData
            return
        }
        dropInViewModel.isWaitingResult = true
        setLoading(true)
        dropInService?.requestDetailsCall(actionComponentData)
    }

    override fun showError(errorMessage: String, reason: String, terminate: Boolean) {
        Logger.d(TAG, "showError - message: $errorMessage")
        AlertDialog.Builder(this)
            .setTitle(R.string.error_dialog_title)
            .setMessage(errorMessage)
            .setOnDismissListener { this@DropInActivity.errorDialogDismissed(reason, terminate) }
            .setPositiveButton(R.string.error_dialog_button) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun errorDialogDismissed(reason: String, terminateDropIn: Boolean) {
        if (terminateDropIn) {
            terminateWithError(reason)
        } else {
            setLoading(false)
        }
    }

    override fun displayAction(action: Action) {
        Logger.d(TAG, "showActionDialog")
        setLoading(false)
        hideAllScreens()
        val actionFragment = ActionComponentDialogFragment.newInstance(action)
        actionFragment.show(supportFragmentManager, ACTION_FRAGMENT_TAG)
        actionFragment.setToHandleWhenStarting()
    }

    override fun onActionError(errorMessage: String) {
        showError(getString(R.string.action_failed), errorMessage, true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Logger.d(TAG, "onSaveInstanceState")
        actionHandler.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        setLoading(dropInViewModel.isWaitingResult)
    }

    override fun showPreselectedDialog() {
        Logger.d(TAG, "showPreselectedDialog")
        hideAllScreens()
        PreselectedStoredPaymentMethodFragment.newInstance(dropInViewModel.preselectedStoredPayment)
            .show(supportFragmentManager, PRESELECTED_PAYMENT_METHOD_FRAGMENT_TAG)
    }

    override fun showPaymentMethodsDialog() {
        Logger.d(TAG, "showPaymentMethodsDialog")
        hideAllScreens()
        PaymentMethodListDialogFragment().show(supportFragmentManager, PAYMENT_METHODS_LIST_FRAGMENT_TAG)
    }

    override fun showStoredComponentDialog(storedPaymentMethod: StoredPaymentMethod, fromPreselected: Boolean) {
        Logger.d(TAG, "showStoredComponentDialog")
        hideAllScreens()
        val dialogFragment = when (storedPaymentMethod.type) {
            PaymentMethodTypes.SCHEME -> CardComponentDialogFragment
            else -> GenericComponentDialogFragment
        }.newInstance(storedPaymentMethod, dropInViewModel.dropInConfiguration, fromPreselected)

        dialogFragment.show(supportFragmentManager, COMPONENT_FRAGMENT_TAG)
    }

    override fun showComponentDialog(paymentMethod: PaymentMethod) {
        Logger.d(TAG, "showComponentDialog")
        hideAllScreens()
        val dialogFragment = when (paymentMethod.type) {
            PaymentMethodTypes.SCHEME -> CardComponentDialogFragment
            PaymentMethodTypes.GIFTCARD -> GiftCardComponentDialogFragment
            else -> GenericComponentDialogFragment
        }.newInstance(paymentMethod, dropInViewModel.dropInConfiguration)

        dialogFragment.show(supportFragmentManager, COMPONENT_FRAGMENT_TAG)
    }

    private fun hideAllScreens() {
        hideFragmentDialog(PRESELECTED_PAYMENT_METHOD_FRAGMENT_TAG)
        hideFragmentDialog(PAYMENT_METHODS_LIST_FRAGMENT_TAG)
        hideFragmentDialog(COMPONENT_FRAGMENT_TAG)
        hideFragmentDialog(ACTION_FRAGMENT_TAG)
        hideFragmentDialog(GIFT_CARD_PAYMENT_CONFIRMATION_FRAGMENT_TAG)
    }

    override fun terminateDropIn() {
        Logger.d(TAG, "terminateDropIn")
        terminateWithError(DropIn.ERROR_REASON_USER_CANCELED)
    }

    override fun startGooglePay(paymentMethod: PaymentMethod, googlePayConfiguration: GooglePayConfiguration) {
        Logger.d(TAG, "startGooglePay")
        googlePayComponent = GooglePayComponent.PROVIDER.get(this, paymentMethod, googlePayConfiguration)
        googlePayComponent.observe(this@DropInActivity, googlePayObserver)
        googlePayComponent.observeErrors(this@DropInActivity, googlePayErrorObserver)

        hideFragmentDialog(PAYMENT_METHODS_LIST_FRAGMENT_TAG)
        googlePayComponent.startGooglePayScreen(this, GOOGLE_PAY_REQUEST_CODE)
    }

    override fun requestBalanceCall(giftCardComponentState: GiftCardComponentState) {
        Logger.d(TAG, "requestCheckBalanceCall")
        val paymentMethod = dropInViewModel.onBalanceCallRequested(giftCardComponentState) ?: return
        if (dropInService == null) {
            Logger.e(TAG, "requestBalanceCall - service is disconnected")
            balanceDataQueue = giftCardComponentState
            return
        }
        dropInViewModel.isWaitingResult = true
        setLoading(true)
        dropInService?.requestBalanceCall(paymentMethod)
    }

    private fun requestOrdersCall() {
        Logger.d(TAG, "requestOrdersCall")
        if (dropInService == null) {
            Logger.e(TAG, "requestOrdersCall - service is disconnected")
            orderDataQueue = Unit
            return
        }
        dropInViewModel.isWaitingResult = true
        setLoading(true)
        dropInService?.requestOrdersCall()
    }

    private fun handleDropInServiceResult(dropInServiceResult: BaseDropInServiceResult) {
        Logger.d(TAG, "handleDropInServiceResult - ${dropInServiceResult::class.simpleName}")
        dropInViewModel.isWaitingResult = false
        when (dropInServiceResult) {
            is DropInServiceResult -> handleDropInServiceResult(dropInServiceResult)
            is BalanceDropInServiceResult -> handleDropInServiceResult(dropInServiceResult)
            is OrderDropInServiceResult -> handleDropInServiceResult(dropInServiceResult)
        }
    }

    private fun handleDropInServiceResult(dropInServiceResult: DropInServiceResult) {
        when (dropInServiceResult) {
            is DropInServiceResult.Finished -> sendResult(dropInServiceResult.result)
            is DropInServiceResult.Action -> handleAction(dropInServiceResult.action)
            is DropInServiceResult.Error -> handleErrorDropInServiceResult(dropInServiceResult)
        }
    }

    private fun handleDropInServiceResult(dropInServiceResult: BalanceDropInServiceResult) {
        when (dropInServiceResult) {
            is BalanceDropInServiceResult.Balance -> handleBalanceResult(dropInServiceResult.balance)
            is BalanceDropInServiceResult.Error -> handleErrorDropInServiceResult(dropInServiceResult)
        }
    }

    private fun handleDropInServiceResult(dropInServiceResult: OrderDropInServiceResult) {
        when (dropInServiceResult) {
            is OrderDropInServiceResult.OrderCreated -> handleOrderResult(dropInServiceResult.order)
            is OrderDropInServiceResult.Error -> handleErrorDropInServiceResult(dropInServiceResult)
        }
    }

    private fun handleErrorDropInServiceResult(dropInServiceResult: DropInServiceResultError) {
        Logger.d(TAG, "handleDropInServiceResult ERROR - reason: ${dropInServiceResult.reason}")
        val reason = dropInServiceResult.reason ?: "Unspecified reason"
        val errorMessage = dropInServiceResult.errorMessage ?: getString(R.string.payment_failed)
        showError(errorMessage, reason, dropInServiceResult.dismissDropIn)
    }

    private fun handleAction(action: Action) {
        actionHandler.handleAction(this, action, ::sendResult)
    }

    private fun sendResult(content: String) {
        val resultHandlerIntent = dropInViewModel.resultHandlerIntent
        // Merchant requested the result to be sent back with a result intent
        if (resultHandlerIntent != null) {
            resultHandlerIntent.putExtra(DropIn.RESULT_KEY, content)
            startActivity(resultHandlerIntent)
        }
        // Merchant did not specify a result intent and should handle the result in onActivityResult
        else {
            val resultIntent = Intent().putExtra(DropIn.RESULT_KEY, content)
            setResult(Activity.RESULT_OK, resultIntent)
        }
        terminateSuccessfully()
    }

    private fun terminateSuccessfully() {
        Logger.d(TAG, "terminateSuccessfully")
        terminate()
    }

    private fun terminateWithError(reason: String) {
        Logger.d(TAG, "terminateWithError")
        val resultIntent = Intent().putExtra(DropIn.ERROR_REASON_KEY, reason)
        setResult(Activity.RESULT_CANCELED, resultIntent)
        terminate()
    }

    private fun terminate() {
        Logger.d(TAG, "terminate")
        finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    private fun handleIntent(intent: Intent) {
        Logger.d(TAG, "handleIntent: action - ${intent.action}")
        dropInViewModel.isWaitingResult = false

        if (WeChatPayUtils.isResultIntent(intent)) {
            Logger.d(TAG, "isResultIntent")
            actionHandler.handleWeChatPayResponse(intent)
        }

        when (intent.action) {
            // Redirect response
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data != null && data.toString().startsWith(RedirectUtil.REDIRECT_RESULT_SCHEME)) {
                    actionHandler.handleRedirectResponse(intent)
                } else {
                    Logger.e(TAG, "Unexpected response from ACTION_VIEW - ${intent.data}")
                }
            }
            else -> {
                Logger.e(TAG, "Unable to find action")
            }
        }
    }

    private fun sendAnalyticsEvent() {
        Logger.d(TAG, "sendAnalyticsEvent")
        val analyticEvent = AnalyticEvent.create(
            this,
            AnalyticEvent.Flavor.DROPIN,
            "dropin",
            dropInViewModel.dropInConfiguration.shopperLocale
        )
        AnalyticsDispatcher.dispatchEvent(this, dropInViewModel.dropInConfiguration.environment, analyticEvent)
    }

    private fun hideFragmentDialog(tag: String) {
        getFragmentByTag(tag)?.dismiss()
    }

    private fun getFragmentByTag(tag: String): DialogFragment? {
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        return fragment as DialogFragment?
    }

    private fun setLoading(showLoading: Boolean) {
        if (showLoading) {
            if (!loadingDialog.isAdded) {
                loadingDialog.show(supportFragmentManager, LOADING_FRAGMENT_TAG)
            }
        } else {
            getFragmentByTag(LOADING_FRAGMENT_TAG)?.dismiss()
        }
    }

    private fun handleBalanceResult(balanceResult: BalanceResult) {
        Logger.v(TAG, "handleBalanceResult")
        val result = dropInViewModel.handleBalanceResult(balanceResult)
        Logger.d(TAG, "handleBalanceResult: ${result::class.java.simpleName}")
        when (result) {
            is GiftCardBalanceResult.Error -> showError(getString(result.errorMessage), result.reason, result.terminateDropIn)
            is GiftCardBalanceResult.FullPayment -> handleGiftCardFullPayment(result)
            is GiftCardBalanceResult.PartialPayment -> handleGiftCardPartialPayment(result)
        }
    }

    private fun handleGiftCardFullPayment(fullPayment: GiftCardBalanceResult.FullPayment) {
        Logger.d(TAG, "handleGiftCardFullPayment")
        setLoading(false)
        showGiftCardPaymentConfirmationDialog(fullPayment.data)
    }

    private fun showGiftCardPaymentConfirmationDialog(data: GiftCardPaymentConfirmationData) {
        Logger.d(TAG, "showGiftCardPaymentConfirmationDialog")
        hideAllScreens()
        GiftCardPaymentConfirmationDialogFragment.newInstance(data)
            .show(supportFragmentManager, GIFT_CARD_PAYMENT_CONFIRMATION_FRAGMENT_TAG)
    }

    private fun handleGiftCardPartialPayment(partialPayment: GiftCardBalanceResult.PartialPayment) {
        Logger.d(TAG, "handleGiftCardPartialPayment")
        setLoading(false)
        requestOrdersCall()
    }

    private fun handleOrderResult(order: OrderResponse) {
        Logger.v(TAG, "handleOrderResult")
        dropInViewModel.handleOrderResponse(order)
        val paymentComponentState = dropInViewModel.cachedGiftCardComponentState
            ?: throw CheckoutException("Lost reference to cached GiftCardComponentState")
        requestPaymentsCall(paymentComponentState)
    }

    companion object {
        fun createIntent(
            context: Context,
            dropInConfiguration: DropInConfiguration,
            paymentMethodsApiResponse: PaymentMethodsApiResponse,
            resultHandlerIntent: Intent?
        ): Intent {
            val intent = Intent(context, DropInActivity::class.java)
            DropInViewModel.putIntentExtras(intent, dropInConfiguration, paymentMethodsApiResponse, resultHandlerIntent)
            return intent
        }
    }
}
