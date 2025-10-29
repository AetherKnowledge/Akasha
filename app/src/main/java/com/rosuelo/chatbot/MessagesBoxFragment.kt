package com.rosuelo.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessagesBoxFragment : Fragment() {

    interface Listener {
        fun onNewChat()
        fun onChatClick(chat: SupabaseProvider.Chat)
        fun onDeleteChat(chat: SupabaseProvider.Chat)
    }

    var listener: Listener? = null
    private var chats: List<SupabaseProvider.Chat> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.messages_box, container, false)
        val btnNew = root.findViewById<View>(R.id.btnNewChat)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerChats)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ChatAdapter(
            onClick = { chat -> listener?.onChatClick(chat) },
            onDelete = { chat -> listener?.onDeleteChat(chat) }
        )
        recycler.adapter = adapter
        // Add vertical spacing between items (10dp)
        val spacingPx = (recycler.resources.displayMetrics.density * 10).toInt()
        recycler.addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State) {
                val pos = parent.getChildAdapterPosition(view)
                if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    outRect.bottom = spacingPx
                }
            }
        })

        btnNew.setOnClickListener { listener?.onNewChat() }

        // Load chats for current user
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val user = getCurrentUser()
            if (user != null) {
                val list = SupabaseProvider.getChats(user.id)
                chats = list
                launch(Dispatchers.Main) { adapter.submitList(list) }
            }
        }

        return root
    }

    fun setChats(list: List<SupabaseProvider.Chat>) {
        view?.findViewById<RecyclerView>(R.id.recyclerChats)?.let { rv ->
            (rv.adapter as? ChatAdapter)?.submitList(list)
        }
        chats = list
    }

    private class ChatAdapter(
        val onClick: (SupabaseProvider.Chat) -> Unit,
        val onDelete: (SupabaseProvider.Chat) -> Unit
    ) : ListAdapter<SupabaseProvider.Chat, ChatViewHolder>(DIFF) {

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<SupabaseProvider.Chat>() {
                override fun areItemsTheSame(a: SupabaseProvider.Chat, b: SupabaseProvider.Chat) = a.id == b.id
                override fun areContentsTheSame(a: SupabaseProvider.Chat, b: SupabaseProvider.Chat) = a == b
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
            return ChatViewHolder(v)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val chat = getItem(position)
            holder.bind(chat, onClick, onDelete)
        }
    }

    private class ChatViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val title = v.findViewById<TextView>(R.id.txtTitle)
        private val subtitle = v.findViewById<TextView>(R.id.txtSubtitle)
        private val delete = v.findViewById<View>(R.id.btnDelete)

        fun bind(chat: SupabaseProvider.Chat, onClick: (SupabaseProvider.Chat) -> Unit, onDelete: (SupabaseProvider.Chat) -> Unit) {
            title.text = chat.title.ifBlank { "Untitled chat" }
            subtitle.text = chat.messages.lastOrNull()?.content ?: "Start a conversation"
            itemView.setOnClickListener { onClick(chat) }
            delete.setOnClickListener { onDelete(chat) }
        }
    }
}
