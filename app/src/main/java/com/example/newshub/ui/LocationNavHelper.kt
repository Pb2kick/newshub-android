package com.example.newshub.ui

import androidx.fragment.app.Fragment
import com.example.newshub.core.location.NewsLocationContext
import com.example.newshub.databinding.PartialTopBarBinding

object LocationNavHelper {

    fun wirePin(
        fragment: Fragment,
        topBar: PartialTopBarBinding,
        onLocationApplied: (NewsLocationContext) -> Unit
    ) {
        topBar.buttonLocation.setOnClickListener {
            val sheet = LocationBottomSheetFragment.newInstance()
            sheet.show(fragment.parentFragmentManager, "location_sheet")
        }
        fragment.parentFragmentManager.setFragmentResultListener(
            LocationBottomSheetFragment.REQUEST_KEY,
            fragment.viewLifecycleOwner
        ) { _, bundle ->
            val ctx = bundle.getSerializable(LocationBottomSheetFragment.BUNDLE_LOCATION) as? NewsLocationContext
                ?: return@setFragmentResultListener
            onLocationApplied(ctx)
        }
    }
}
