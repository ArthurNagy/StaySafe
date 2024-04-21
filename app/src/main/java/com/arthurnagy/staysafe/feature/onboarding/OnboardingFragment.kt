package com.arthurnagy.staysafe.feature.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arthurnagy.staysafe.OnboardingBinding
import com.arthurnagy.staysafe.R
import com.arthurnagy.staysafe.feature.shared.addPageChangeListenerTo

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = OnboardingBinding.bind(view)
        with(binding) {
            skip.setOnClickListener {
                findNavController().navigateUp()
            }
            val onboardingPagerAdapter = OnboardingPagerAdapter()
            pager.adapter = onboardingPagerAdapter
            pager.setPageTransformer(false) { _, _ ->
                // TODO: Add animation
            }
            indicator.attachTo(pager)
            addPageChangeListenerTo(pager, onPageSelected = {
                skip.text = getString(if (it == onboardingPagerAdapter.count - 1) R.string.start else R.string.skip)
            })
        }
    }
}