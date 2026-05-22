package com.example.newshub.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newshub.R

object BottomNavHelper {

    fun wire(
        fragment: Fragment,
        navHome: View,
        navElections: View,
        navAlerts: View,
        navProfile: View
    ) {
        val controller = fragment.findNavController()

        navHome.setOnClickListener {
            if (controller.currentDestination?.id != R.id.homeFragment) {
                controller.navigate(R.id.homeFragment)
            }
        }

        navElections.setOnClickListener {
            if (controller.currentDestination?.id != R.id.electionsFragment) {
                controller.navigate(R.id.electionsFragment)
            }
        }

        navAlerts.setOnClickListener {
            if (controller.currentDestination?.id != R.id.notificationsFragment) {
                controller.navigate(R.id.notificationsFragment)
            }
        }

        navProfile.setOnClickListener {
            if (controller.currentDestination?.id != R.id.profileFragment) {
                controller.navigate(R.id.profileFragment)
            }
        }
    }
}
