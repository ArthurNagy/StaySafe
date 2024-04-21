package com.arthurnagy.staysafe.feature.newdocument.signature

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.Slide
import com.arthurnagy.staysafe.R
import com.arthurnagy.staysafe.SignatureBinding
import com.arthurnagy.staysafe.feature.newdocument.NewDocumentViewModel
import com.arthurnagy.staysafe.feature.shared.doIfAboveVersion
import org.koin.androidx.navigation.koinNavGraphViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SignatureFragment : Fragment(R.layout.fragment_signature) {

    private val sharedViewModel by koinNavGraphViewModel<NewDocumentViewModel>(navGraphId = R.id.nav_new_document)
    private val viewModel: SignatureViewModel by viewModel { parametersOf(sharedViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doIfAboveVersion(Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O && Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
                enterTransition = Slide(Gravity.END)
                sharedElementEnterTransition = ChangeBounds()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SignatureBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@SignatureFragment.viewModel
        }
        with(binding) {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            clear.setOnClickListener {
                signaturePad.clear()
            }
        }
        viewModel.events.observe(viewLifecycleOwner) {
            when (val action = it.consume()) {
                is SignatureViewModel.Action.OpenDocument -> findNavController().navigate(
                    SignatureFragmentDirections.actionGlobalDocumentDetailFragment(action.documentId)
                )

                null -> Unit
            }
        }
    }
}