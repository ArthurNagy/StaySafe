package com.arthurnagy.staysafe.feature.newdocument.statement.type

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.Slide
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.arthurnagy.staysafe.R
import com.arthurnagy.staysafe.StatementTypeBinding
import com.arthurnagy.staysafe.feature.newdocument.NewDocumentViewModel
import com.arthurnagy.staysafe.feature.shared.InAppPurchaseHelper
import com.arthurnagy.staysafe.feature.shared.OnDisconnected
import com.arthurnagy.staysafe.feature.shared.doIfAboveVersion
import com.arthurnagy.staysafe.feature.shared.showSnackbar
import org.koin.androidx.navigation.koinNavGraphViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class StatementTypeFragment : Fragment(R.layout.fragment_statement_type) {

    private val sharedViewModel: NewDocumentViewModel by koinNavGraphViewModel(navGraphId = R.id.nav_new_document)
    private val viewModel: StatementTypeViewModel by viewModel { parametersOf(sharedViewModel) }
    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(requireContext())
            .setListener(createPurchaseListener())
            .enablePendingPurchases()
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doIfAboveVersion(Build.VERSION_CODES.LOLLIPOP_MR1) {
            exitTransition = Slide(Gravity.START)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = StatementTypeBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@StatementTypeFragment.viewModel
        }

        with(binding) {
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
            statementTypeOne.root.setOnClickListener {
                setStatementStartHourAndNavigateForward(START_HOUR_EIGHT_PM, this)
            }
            statementTypeTwo.root.setOnClickListener {
                setStatementStartHourAndNavigateForward(START_HOUR_TEN_PM, this)
            }
            comingSoon.setOnClickListener {
                startPurchaseFlow {
                    Timber.e("startPurchaseFlow: onDisconnected")
                    showSnackbar(view = binding.root, message = R.string.in_app_purchase_connection_failed)
                }
            }
        }
    }

    private fun setStatementStartHourAndNavigateForward(startHour: Int, binding: StatementTypeBinding) {
        viewModel.updateStatementStartHour(startHour)
        findNavController().navigate(
            StatementTypeFragmentDirections.actionStatementTypeFragmentToStatementPersonalDataFragment(),
            FragmentNavigatorExtras(
                binding.toolbar to getString(R.string.transition_toolbar)
            )
        )
    }

    private fun startPurchaseFlow(onDisconnected: OnDisconnected) {
        InAppPurchaseHelper.startPurchaseFlow(
            billingClient = billingClient,
            onConnected = {
                Timber.d("startPurchaseFlow: onConnected: ${it.responseCode}")
                lifecycleScope.launchWhenResumed {
                    InAppPurchaseHelper.launchBillingFlow(billingClient, requireActivity())
                }
            },
            onDisconnected = onDisconnected
        )
    }

    private fun createPurchaseListener(): PurchasesUpdatedListener = object : InAppPurchaseHelper.SimplePurchaseListener() {

        override fun onPurchase(purchases: MutableList<Purchase>) {
            Timber.d("createPurchaseListener: onPurchase: $purchases")
            lifecycleScope.launchWhenResumed {
                view?.let {
                    (DataBindingUtil.getBinding(it) ?: DataBindingUtil.bind<StatementTypeBinding>(it))?.let { binding ->
                        purchases.forEach {
                            when (InAppPurchaseHelper.consumePurchase(billingClient, it)) {
                                InAppPurchaseHelper.PurchaseResult.Success -> showSnackbar(
                                    view = binding.root,
                                    message = R.string.in_app_purchase_success
                                )
                                InAppPurchaseHelper.PurchaseResult.Pending -> showSnackbar(
                                    view = binding.root,
                                    message = R.string.in_app_purchase_pending
                                )
                                InAppPurchaseHelper.PurchaseResult.Error -> showSnackbar(
                                    view = binding.root,
                                    message = R.string.in_app_purchase_error
                                )
                                InAppPurchaseHelper.PurchaseResult.Ignored -> Unit
                            }
                        }
                    }
                }
            }
        }

        override fun onError(purchaseResult: BillingResult) {
            Timber.e("createPurchaseListener: onError: $purchaseResult")
            view?.let {
                (DataBindingUtil.getBinding(it) ?: DataBindingUtil.bind<StatementTypeBinding>(it))?.let { binding ->
                    showSnackbar(view = binding.root, message = R.string.in_app_purchase_error)
                }
            }
        }
    }

    companion object {
        private const val START_HOUR_EIGHT_PM = 20
        private const val START_HOUR_TEN_PM = 22
    }
}