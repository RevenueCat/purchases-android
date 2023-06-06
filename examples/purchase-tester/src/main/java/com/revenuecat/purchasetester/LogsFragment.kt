package com.revenuecat.purchasetester

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases_sample.databinding.FragmentLogsBinding
import com.revenuecat.purchases_sample.databinding.LogRowViewBinding

class LogsFragment : Fragment() {

    lateinit var binding: FragmentLogsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogsBinding.inflate(inflater)

        NavigationUI.setupWithNavController(binding.logsToolbar, findNavController())
        setupLogsRecyclerView()
        return binding.root
    }

    private fun setupLogsRecyclerView() {
        val application = (requireActivity().application as MainApplication)
        binding.logsRecyclerView.adapter = LogsAdapter(application.logHandler.storedLogs)
    }

    private class LogsAdapter(
        private val logs: List<LogMessage>
    ) : ListAdapter<LogMessage, ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LogRowViewBinding.inflate(LayoutInflater.from(parent.context)))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val logMessage = logs[position]
            holder.binding.logMessageTextView.text = logMessage.message
            holder.binding.logMessageContainer.setBackgroundColor(
                ContextCompat.getColor(holder.binding.root.context, logMessage.logLevel.colorResource)
            )
        }

        override fun getItemCount(): Int {
            return logs.size
        }
    }

    private class ViewHolder(val binding: LogRowViewBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<LogMessage>() {
        override fun areItemsTheSame(oldItem: LogMessage, newItem: LogMessage): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: LogMessage, newItem: LogMessage): Boolean {
            return oldItem == newItem
        }
    }
}
