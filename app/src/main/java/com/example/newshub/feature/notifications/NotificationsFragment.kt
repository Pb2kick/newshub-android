package com.example.newshub.feature.notifications

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newshub.R
import com.example.newshub.core.session.AndroidSessionStore
import com.example.newshub.databinding.FragmentNotificationsBinding
import com.example.newshub.ui.BottomNavHelper

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationsViewModel by viewModels {
        NotificationsViewModelFactory(AndroidSessionStore(requireContext().applicationContext))
    }
    private lateinit var adapter: NotificationAdapter

    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            viewModel.refresh()
            pollHandler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NotificationAdapter { item -> viewModel.deleteNotification(item.id) }
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifications.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    viewModel.deleteNotification(adapter.currentList[position].id)
                }
            }
        }).attachToRecyclerView(binding.recyclerNotifications)

        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
        binding.buttonMarkAllRead.setOnClickListener { viewModel.markAllRead() }
        binding.buttonFilterAll.setOnClickListener { selectFilter(unreadOnly = false) }
        binding.buttonFilterUnread.setOnClickListener { selectFilter(unreadOnly = true) }

        BottomNavHelper.wire(
            fragment = this,
            navHome = binding.bottomNavBar.navHome,
            navElections = binding.bottomNavBar.navElections,
            navAlerts = binding.bottomNavBar.navAlerts,
            navProfile = binding.bottomNavBar.navProfile
        )
        binding.bottomNavBar.navAlerts.setOnClickListener { }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressNotifications.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            adapter.submitList(state.items)
            binding.textEmpty.visibility = if (state.items.isEmpty() && !state.isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
            if (state.unreadCount > 0) {
                binding.badgeUnreadCount.visibility = View.VISIBLE
                binding.badgeUnreadCount.text = if (state.unreadCount > 9) "9+" else state.unreadCount.toString()
            } else {
                binding.badgeUnreadCount.visibility = View.GONE
            }
            state.messageRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        viewModel.refresh()
    }

    private fun selectFilter(unreadOnly: Boolean) {
        viewModel.setUnreadOnly(unreadOnly)
        val context = requireContext()
        if (unreadOnly) {
            binding.buttonFilterAll.backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.transparent)
            binding.buttonFilterAll.setTextColor(ContextCompat.getColor(context, R.color.home_text_secondary))
            binding.buttonFilterAll.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.buttonFilterUnread.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.home_card_bg)
            binding.buttonFilterUnread.setTextColor(ContextCompat.getColor(context, R.color.home_text_primary))
            binding.buttonFilterUnread.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            binding.buttonFilterUnread.backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.transparent)
            binding.buttonFilterUnread.setTextColor(ContextCompat.getColor(context, R.color.home_text_secondary))
            binding.buttonFilterUnread.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.buttonFilterAll.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.home_card_bg)
            binding.buttonFilterAll.setTextColor(ContextCompat.getColor(context, R.color.home_text_primary))
            binding.buttonFilterAll.setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    override fun onResume() {
        super.onResume()
        pollHandler.postDelayed(pollRunnable, 60_000L)
    }

    override fun onPause() {
        pollHandler.removeCallbacks(pollRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        pollHandler.removeCallbacks(pollRunnable)
        super.onDestroyView()
        _binding = null
    }
}
