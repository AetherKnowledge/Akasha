package com.rosuelo.chatbot

import android.graphics.LinearGradient
import android.graphics.Shader
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
    private var displayName: String = "User"
    private var greetingTextView: TextView? = null
    private var userIdArg: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.new_chat, container, false)

    val greeting = view.findViewById<TextView>(R.id.txtGreeting)
    greetingTextView = greeting
    val edtQuery = view.findViewById<EditText>(R.id.edtQuery)
        val btnAsk = view.findViewById<Button>(R.id.btnAsk)
        val progress = view.findViewById<ProgressBar>(R.id.progressAsk)
        val contentRow = view.findViewById<View>(R.id.contentRow)

        updateGreetingText(displayName)

        btnAsk.setOnClickListener {
            if (progress.visibility == View.VISIBLE) return@setOnClickListener
            val text = edtQuery.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener

            // Show centered loader, hide the row
            progress.visibility = View.VISIBLE
            contentRow.visibility = View.INVISIBLE
            btnAsk.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val chat = createNewChatandSendMessage(
                    userId = (userIdArg ?: arguments?.getString(ARG_USER_ID)) ?: return@launch,
                    prompt = text
                )
                edtQuery.setText("")
                progress.visibility = View.GONE
                contentRow.visibility = View.VISIBLE
                btnAsk.isEnabled = true
                listener?.onNewChatCreated(chat)
            }
        }

        return view
    }

    fun setDisplayName(name: String) {
        updateGreetingText(name)
    }

    fun setUserId(userId: String) {
        userIdArg = userId
    }

    private fun updateGreetingText(name: String) {
        // store for view-less state
        displayName = name
        val g = greetingTextView ?: return
        g.text = "Hello, $name!"
        // Apply gradient after layout
        g.post {
            val height = g.height.toFloat()
            if (height > 0f) {
                val primary = requireContext().getColor(R.color.ak_primary)
                val secondary = requireContext().getColor(R.color.ak_secondary)
                val shader = LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(secondary, primary),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                g.paint.shader = shader
                g.invalidate()
            }
        }
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
