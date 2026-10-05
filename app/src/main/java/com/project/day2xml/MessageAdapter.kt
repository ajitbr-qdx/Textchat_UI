package com.project.day2xml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.day2xml.databinding.ItemMessageAttachmentBinding
import com.project.day2xml.databinding.ItemMessageReceivedBinding
import com.project.day2xml.databinding.ItemMessageSentBinding
import com.project.day2xml.databinding.ItemMessageSystemBinding

class MessageAdapter(
    private val onMessageClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }

        private const val VIEW_TYPE_SENT = 0
        private const val VIEW_TYPE_RECEIVED = 1
        private const val VIEW_TYPE_ATTACHMENT = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    class SentViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvSentSender.text = if (message.sender.isNotEmpty()) message.sender else "You"
            binding.tvSentMessage.text = message.text
        }
    }

    class ReceivedViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvReceivedSender.text = if (message.sender.isNotEmpty()) message.sender else "Echo Server"
            binding.tvReceivedMessage.text = message.text
        }
    }

    class AttachmentViewHolder(
        private val binding: ItemMessageAttachmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvAttachmentName.text = "📎 ${message.text}"
        }
    }

    class SystemViewHolder(
        private val binding: ItemMessageSystemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvSystemMessage.text = message.text
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            MessageType.TEXT_SENT -> VIEW_TYPE_SENT
            MessageType.TEXT_RECEIVED -> VIEW_TYPE_RECEIVED
            MessageType.ATTACHMENT -> VIEW_TYPE_ATTACHMENT
            MessageType.SYSTEM -> VIEW_TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
                SentViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
                ReceivedViewHolder(binding)
            }
            VIEW_TYPE_ATTACHMENT -> {
                val binding = ItemMessageAttachmentBinding.inflate(inflater, parent, false)
                AttachmentViewHolder(binding)
            }
            VIEW_TYPE_SYSTEM -> {
                val binding = ItemMessageSystemBinding.inflate(inflater, parent, false)
                SystemViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
            is AttachmentViewHolder -> holder.bind(message)
            is SystemViewHolder -> holder.bind(message)
        }

        holder.itemView.setOnClickListener {
            onMessageClick(message)
        }
    }
}
