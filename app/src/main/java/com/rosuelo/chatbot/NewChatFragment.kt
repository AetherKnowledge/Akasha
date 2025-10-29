package com.rosuelo.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class NewChatFragment : Fragment() {

    interface Listener {
        fun onNewChatCreated(chat: SupabaseProvider.Chat?)
    }

    var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.new_chat, container, false)

        val greeting = view.findViewById<TextView>(R.id.txtGreeting)
        val edtQuery = view.findViewById<EditText>(R.id.edtQuery)
        val btnAsk = view.findViewById<Button>(R.id.btnAsk)
        val progress = view.findViewById<ProgressBar>(R.id.progressAsk)

        // Set greeting text with gradient is not feasible in plain TextView; we fall back to solid color.
        val name = arguments?.getString(ARG_DISPLAY_NAME) ?: "User"
        greeting.text = "Hello, $name!"

        btnAsk.setOnClickListener {
            if (progress.visibility == View.VISIBLE) return@setOnClickListener
            val text = edtQuery.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener

            progress.visibility = View.VISIBLE
            btnAsk.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val chat = createNewChatandSendMessage(
                    userId = requireArguments().getString(ARG_USER_ID) ?: return@launch,
                    prompt = text
                )
                edtQuery.setText("")
                progress.visibility = View.GONE
                btnAsk.isEnabled = true
                listener?.onNewChatCreated(chat)
            }
        }

        return view
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_DISPLAY_NAME = "display_name"

        fun newInstance(userId: String, displayName: String): NewChatFragment {
            val f = NewChatFragment()
            f.arguments = bundleOf(
                ARG_USER_ID to userId,
                ARG_DISPLAY_NAME to displayName
            )
            return f
        }
    }
}
