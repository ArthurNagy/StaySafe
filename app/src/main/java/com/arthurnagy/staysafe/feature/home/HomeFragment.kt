package com.arthurnagy.staysafe.feature.home

import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.arthurnagy.staysafe.HomeBinding
import com.arthurnagy.staysafe.R
import com.arthurnagy.staysafe.core.PreferenceManager
import com.arthurnagy.staysafe.feature.shared.InAppPurchaseHelper
import com.arthurnagy.staysafe.feature.shared.OnDisconnected
import com.arthurnagy.staysafe.feature.shared.consume
import com.arthurnagy.staysafe.feature.shared.setupSwipeToDelete
import com.arthurnagy.staysafe.feature.shared.showSnackbar
import com.arthurnagy.staysafe.feature.shared.updateMargins
import com.google.android.material.datepicker.MaterialDatePicker
import dev.chrisbanes.insetter.Insetter
import org.koin.android.ext.android.inject
import org.koin.androidx.navigation.koinNavGraphViewModel
import timber.log.Timber

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val preferenceManager by inject<PreferenceManager>()
    private val viewModel by koinNavGraphViewModel<HomeViewModel>(navGraphId = R.id.nav_main)
    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(requireContext())
            .setListener(createPurchaseListener())
            .enablePendingPurchases()
            .build()
    }
    private var snackbarShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (preferenceManager.shouldShowOnboarding) {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToOnboardingFragment())
            preferenceManager.shouldShowOnboarding = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = HomeBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@HomeFragment.viewModel
        }
        val documentsAdapter = DocumentsAdapter(
            onStatementSelected = { findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToDocumentDetailFragment(it.id)) },
            onStatementDateUpdate = { viewModel.updateStatementDate(MaterialDatePicker.todayInUtcMilliseconds(), it) }
        )
        with(binding) {
            toolbar.setOnMenuItemClickListener {
                consume { findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToOptionsBottomSheet()) }
            }
            add.setOnClickListener { findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToNewDocument()) }

            with(recycler) {
                layoutManager = LinearLayoutManager(requireContext())
                if (itemDecorationCount == 0) {
                    val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                    val firstKeyline = resources.getDimensionPixelSize(R.dimen.first_keyline)
                    itemDecoration.setDrawable(InsetDrawable(itemDecoration.drawable?.mutate(), firstKeyline, 0, firstKeyline, 0))
                    addItemDecoration(itemDecoration)
                }
                adapter = documentsAdapter
            }

            setupSwipeToDelete(recycler, documentsAdapter, onRemoveItem = { viewModel?.deleteStatement(it.statement) })
        }

        Insetter.builder()
            .setOnApplyInsetsListener { confirmView, insets, initialState ->
                val bottomInsetMargin = insets.systemWindowInsetBottom
                val viewBottomMargin = if (snackbarShown) initialState.margins.bottom else bottomInsetMargin + initialState.margins.bottom
                confirmView.updateMargins(marginBottom = viewBottomMargin.toFloat())
            }
            .consume(Insetter.CONSUME_AUTO)
            .applyToView(binding.add)


        with(viewModel) {
            items.observe(viewLifecycleOwner, documentsAdapter::submitList)
            statementDeletedEvent.observe(viewLifecycleOwner) {
                it.consume()?.let { statement ->
                    displaySnackbar(view = binding.coordinator, addButton = binding.add, message = R.string.form_deleted_message, action = R.string.undo) {
                        viewModel.undoStatementDeletion(statement)
                    }
                }
            }
            statementDateUpdatedEvent.observe(viewLifecycleOwner) {
                it.consume()?.let {
                    displaySnackbar(view = binding.coordinator, addButton = binding.add, message = R.string.statement_date_updated)
                }
            }
            purchaseEvent.observe(viewLifecycleOwner) {
                it.consume()?.let {
                    startPurchaseFlow {
                        Timber.e("startPurchaseFlow: onDisconnected")
                        displaySnackbar(view = binding.coordinator, addButton = binding.add, message = R.string.in_app_purchase_connection_failed)
                    }
                }
            }
        }
        InAppPurchaseHelper.checkPurchases(billingClient, onConnected = {
            Timber.d("checkPurchases: onConnected: $it")
            lifecycleScope.launchWhenResumed {
                InAppPurchaseHelper.consumeAlreadyPurchased(billingClient)
            }
        }, onDisconnected = {
            Timber.e("checkPurchases: onDisconnected")
            // TODO: check if we need to handle this ATM
        })
    }

    private fun displaySnackbar(view: View, addButton: View, message: Int, action: Int = 0, func: (() -> Unit)? = null) {
        snackbarShown = true
        showSnackbar(view = view, message = message, action = action, func = func, onDismissed = {
            snackbarShown = false
            ViewCompat.requestApplyInsets(addButton)
        })
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
                    (DataBindingUtil.getBinding(it) ?: DataBindingUtil.bind<HomeBinding>(it))?.let { binding ->
                        purchases.forEach {
                            when (InAppPurchaseHelper.consumePurchase(billingClient, it)) {
                                InAppPurchaseHelper.PurchaseResult.Success -> displaySnackbar(
                                    view = binding.coordinator,
                                    addButton = binding.add,
                                    message = R.string.in_app_purchase_success
                                )
                                InAppPurchaseHelper.PurchaseResult.Pending -> displaySnackbar(
                                    view = binding.coordinator,
                                    addButton = binding.add,
                                    message = R.string.in_app_purchase_pending
                                )
                                InAppPurchaseHelper.PurchaseResult.Error -> displaySnackbar(
                                    view = binding.coordinator,
                                    addButton = binding.add,
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
                (DataBindingUtil.getBinding(it) ?: DataBindingUtil.bind<HomeBinding>(it))?.let { binding ->
                    displaySnackbar(view = binding.coordinator, addButton = binding.add, message = R.string.in_app_purchase_error)
                }
            }
        }
    }
}