package com.rosuelo.chatbot

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RegisterFragment : Fragment() {

    interface Listener {
        fun onAuthSuccess(userData: UserData)
    }

    var listener: Listener? = null

    private var isLogin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.register_screen, container, false)

        // Apply top insets to avoid cutout overlap using baseline padding technique
        val root = view.findViewById<View>(R.id.registerRoot)
        val initialTop = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, initialTop + sys.top, v.paddingRight, v.paddingBottom)
            insets
        }

        bindUi(view)
        return view
    }

    private fun bindUi(view: View) {
        val txtSubtitle = view.findViewById<TextView>(R.id.txtSubtitle)
        val btnPrimary = view.findViewById<Button>(R.id.btnPrimary)
        val txtOr = view.findViewById<TextView>(R.id.txtOr)
        val txtToggle = view.findViewById<TextView>(R.id.txtToggle)
        val edtEmail = view.findViewById<EditText>(R.id.edtEmail)
        val edtPassword = view.findViewById<EditText>(R.id.edtPassword)
        val btnTogglePassword = view.findViewById<ImageButton>(R.id.btnTogglePassword)
        val txtError = view.findViewById<TextView>(R.id.txtError)
        val btnGoogle = view.findViewById<ImageButton>(R.id.btnGoogle)

        fun refreshTexts() {
            txtSubtitle.text = if (isLogin) getString(R.string.login_to_your_account) else getString(R.string.create_new_account)
            btnPrimary.text = if (isLogin) getString(R.string.login) else getString(R.string.sign_up)
            txtOr.text = if (isLogin) getString(R.string.or_log_in_with) else getString(R.string.or_sign_up_with)
            txtToggle.text = if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Log In"
        }
        refreshTexts()

        btnTogglePassword.setOnClickListener {
            val currentlyVisible = (edtPassword.inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            if (currentlyVisible) {
                // Switch to hidden password and show the "show" action icon (eye)
                edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_24)
                btnTogglePassword.contentDescription = getString(R.string.show_password)
            } else {
                // Switch to visible password and show the "hide" action icon (visibility off)
                edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off_24)
                btnTogglePassword.contentDescription = getString(R.string.hide_password)
            }
            // Keep cursor at end after toggling
            edtPassword.setSelection(edtPassword.text?.length ?: 0)
        }

        val authManager = AuthManager(requireContext())

        btnPrimary.setOnClickListener {
            val email = edtEmail.text?.toString()?.trim().orEmpty()
            val password = edtPassword.text?.toString().orEmpty()
            val flow = if (isLogin) authManager.signInWithEmail(email, password) else authManager.signUpWithEmail(email, password)
            flow.onEach { result ->
                when (result) {
                    is AuthResponse.Success -> {
                        txtError.visibility = View.GONE
                        listener?.onAuthSuccess(result.userData)
                    }
                    is AuthResponse.Error -> {
                        txtError.text = result.message ?: getString(R.string.unknown_error)
                        txtError.visibility = View.VISIBLE
                    }
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)
        }

        btnGoogle.setOnClickListener {
            authManager.loginGoogleUser().onEach { result ->
                when (result) {
                    is AuthResponse.Success -> {
                        txtError.visibility = View.GONE
                        listener?.onAuthSuccess(result.userData)
                    }
                    is AuthResponse.Error -> {
                        txtError.text = result.message ?: getString(R.string.unknown_error)
                        txtError.visibility = View.VISIBLE
                    }
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)
        }

        txtToggle.setOnClickListener {
            isLogin = !isLogin
            refreshTexts()
        }
    }
}
