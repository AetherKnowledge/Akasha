package com.rosuelo.chatbot

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatBoxFragment : Fragment() {

    interface Listener {
        fun onUpdateChat(chat: SupabaseProvider.Chat)
    }

    var listener: Listener? = null
    private var chatArg: SupabaseProvider.Chat? = null
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatArg = arguments?.getParcelable("chat")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.chat_box, container, false)
        markwon = Markwon.create(requireContext())

        val recycler = v.findViewById<RecyclerView>(R.id.recyclerMessages)
        val input = v.findViewById<EditText>(R.id.editMessage)
        val btnSend = v.findViewById<FrameLayout>(R.id.btnSend)
        val iconSend = v.findViewById<ImageView>(R.id.iconSend)
        val progress = v.findViewById<ProgressBar>(R.id.progressSend)

        val adapter = MessageAdapter(markwon)
        recycler.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        recycler.adapter = adapter

        val chat = chatArg ?: return v
        adapter.submitList(chat.messages.toList())
        recycler.scrollToPosition(adapter.itemCount - 1)

        btnSend.setOnClickListener {
            if (progress.visibility == View.VISIBLE) return@setOnClickListener
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener

            // Add user message immediately
            val updated = chat.copy(messages = (chat.messages + SupabaseProvider.ChatMessage(SupabaseProvider.MessageType.HUMAN, text)).toMutableList())
            chatArg = updated
            adapter.submitList(updated.messages.toList())
            recycler.scrollToPosition(adapter.itemCount - 1)
            input.setText("")

            // Send and add AI reply
            progress.visibility = View.VISIBLE
            iconSend.visibility = View.GONE
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val reply = ChatBotProvider.sendChatMessage(updated.id, text)
                launch(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    iconSend.visibility = View.VISIBLE
                }
                if (reply != null) {
                    val updated2 = updated.copy(messages = (updated.messages + reply).toMutableList())
                    chatArg = updated2
                    launch(Dispatchers.Main) {
                        adapter.submitList(updated2.messages.toList())
                        recycler.scrollToPosition(adapter.itemCount - 1)
                        listener?.onUpdateChat(updated2)
                    }
                }
            }
        }

        return v
    }

    class MessageAdapter(private val markwon: Markwon) : ListAdapter<SupabaseProvider.ChatMessage, RecyclerView.ViewHolder>(DIFF) {
        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<SupabaseProvider.ChatMessage>() {
                override fun areItemsTheSame(a: SupabaseProvider.ChatMessage, b: SupabaseProvider.ChatMessage) = a === b
                override fun areContentsTheSame(a: SupabaseProvider.ChatMessage, b: SupabaseProvider.ChatMessage) = a == b
            }
            private const val TYPE_AI = 1
            private const val TYPE_HUMAN = 2
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position).type) {
                SupabaseProvider.MessageType.AI -> TYPE_AI
                SupabaseProvider.MessageType.HUMAN -> TYPE_HUMAN
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_AI) {
                val v = inflater.inflate(R.layout.item_message_ai, parent, false)
                AiHolder(v, markwon)
            } else {
                val v = inflater.inflate(R.layout.item_message_human, parent, false)
                HumanHolder(v, markwon)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = getItem(position)
            when (holder) {
                is AiHolder -> holder.bind(msg)
                is HumanHolder -> holder.bind(msg)
            }
        }

        private class AiHolder(itemView: View, val markwon: Markwon) : RecyclerView.ViewHolder(itemView) {
            private val text = itemView.findViewById<android.widget.TextView>(R.id.txtMarkdown)
            fun bind(msg: SupabaseProvider.ChatMessage) { markwon.setMarkdown(text, msg.content) }
        }
        private class HumanHolder(itemView: View, val markwon: Markwon) : RecyclerView.ViewHolder(itemView) {
            private val text = itemView.findViewById<android.widget.TextView>(R.id.txtMarkdown)
            fun bind(msg: SupabaseProvider.ChatMessage) { markwon.setMarkdown(text, msg.content) }
        }
    }

    companion object {
        fun newInstance(chat: SupabaseProvider.Chat): ChatBoxFragment {
            val f = ChatBoxFragment()
            val b = Bundle()
            // Use a simple serializable copy (Parcelable would be better, but keeping simple here)
            b.putString("chat_id", chat.id)
            f.arguments = b
            f.chatArg = chat
            return f
        }
    }
}
