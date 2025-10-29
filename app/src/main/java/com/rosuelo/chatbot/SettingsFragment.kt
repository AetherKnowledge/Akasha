package com.rosuelo.chatbot

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.graphics.BitmapFactory
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    interface Listener {
        fun onBack()
        fun onLogout()
        fun onUserUpdate(userData: UserData)
    }

    var listener: Listener? = null
    private var userArg: UserData? = null
    private var updating: Boolean = false
    private var pickedImageData: ImageData? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && isAdded) {
            val user = userArg ?: return@registerForActivityResult
            // Show selected image immediately
            view?.findViewById<ImageView>(R.id.imgProfile)?.apply {
                setImageURI(uri)
                visibility = View.VISIBLE
            }
            view?.findViewById<TextView>(R.id.txtInitial)?.visibility = View.GONE

            // Read bytes and upload
            updating = true
            viewLifecycleOwner.lifecycleScope.launch {
                val data = readBytesAndMime(requireContext(), uri)
                pickedImageData = data
                val updated = SupabaseProvider.updateProfile(user, name = null, imageData = data)
                if (updated != null) {
                    userArg = updated
                    listener?.onUserUpdate(updated)
                }
                updating = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
    val id = arguments?.getString(ARG_USER_ID)
    val email = arguments?.getString(ARG_USER_EMAIL)
    val name = arguments?.getString(ARG_USER_NAME)
    val avatar = arguments?.getString(ARG_USER_AVATAR)
    userArg = if (id != null && email != null) UserData(id = id, email = email, name = name, avatar = avatar) else null
        val view = inflater.inflate(R.layout.settings_screen, container, false)

        // Insets handling top padding like Register
        val root = view.findViewById<View>(R.id.settingsRoot)
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
    val img = view.findViewById<ImageView>(R.id.imgProfile)
        val txtInitial = view.findViewById<TextView>(R.id.txtInitial)
        val txtDisplay = view.findViewById<TextView>(R.id.txtDisplayName)
        val edtDisplay = view.findViewById<EditText>(R.id.edtDisplayName)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEditName)
    val avatarContainer = view.findViewById<FrameLayout>(R.id.profileImageContainer)
    val rowWeb = view.findViewById<LinearLayout>(R.id.rowWebSearch)
    val switchWeb = view.findViewById<Switch>(R.id.switchWebSearch)
    val btnReturn = view.findViewById<View>(R.id.btnReturn)
    val btnLogout = view.findViewById<View>(R.id.btnLogout)

        val context = requireContext()

        // Load user
    val user = userArg
        val displayName = user?.name ?: user?.email?.substringBefore('@') ?: "User"
        txtDisplay.text = displayName

        if (!user?.avatar.isNullOrBlank()) {
            // Show initial as placeholder while loading
            val firstLetter = user?.name?.firstOrNull()?.uppercaseChar()?.toString()
                ?: user?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            txtInitial.text = firstLetter
            txtInitial.visibility = View.VISIBLE
            img.visibility = View.INVISIBLE

            val url = user!!.avatar!!
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    URL(url).openStream().use { input ->
                        val bmp = BitmapFactory.decodeStream(input)
                        if (bmp != null && isAdded) {
                            launch(Dispatchers.Main) {
                                img.setImageBitmap(bmp)
                                txtInitial.visibility = View.GONE
                                img.visibility = View.VISIBLE
                            }
                        } else if (isAdded) {
                            launch(Dispatchers.Main) {
                                img.visibility = View.INVISIBLE
                                txtInitial.visibility = View.VISIBLE
                            }
                        }
                    }
                } catch (_: Throwable) {
                    if (isAdded) {
                        launch(Dispatchers.Main) {
                            img.visibility = View.INVISIBLE
                            txtInitial.visibility = View.VISIBLE
                        }
                    }
                }
            }
        } else {
            txtInitial.visibility = View.VISIBLE
            img.visibility = View.INVISIBLE
            txtInitial.text = user?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }

        // Pick new image on avatar tap
        avatarContainer.setOnClickListener {
            if (!updating && user != null) {
                imagePicker.launch("image/*")
            }
        }

        // Toggle name edit
        var editing = false
        btnEdit.setOnClickListener {
            editing = !editing
            if (editing) {
                edtDisplay.setText(txtDisplay.text)
                txtDisplay.visibility = View.GONE
                edtDisplay.visibility = View.VISIBLE
                edtDisplay.requestFocus()
                edtDisplay.setSelection(edtDisplay.text.length)
                edtDisplay.inputType = InputType.TYPE_CLASS_TEXT
            } else {
                val newName = edtDisplay.text?.toString()?.trim()
                if (!newName.isNullOrEmpty() && newName != user?.name) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val updated = SupabaseProvider.updateProfile(user!!, name = newName, imageData = pickedImageData)
                        if (updated != null) {
                            txtDisplay.text = updated.name
                            listener?.onUserUpdate(updated)
                            userArg = updated
                        }
                    }
                }
                edtDisplay.visibility = View.GONE
                txtDisplay.visibility = View.VISIBLE
            }
        }

        // Tools toggle wired to Settings
        switchWeb.isChecked = Settings.enabledTools.contains(Tools.WEBSEARCH)
        switchWeb.setOnCheckedChangeListener { _, checked ->
            viewLifecycleOwner.lifecycleScope.launch {
                Settings.toggle(context, Tools.WEBSEARCH, checked)
            }
        }

        btnReturn.setOnClickListener { listener?.onBack() }
        btnLogout.setOnClickListener { listener?.onLogout() }
    }

    private suspend fun readBytesAndMime(context: android.content.Context, uri: Uri): ImageData {
        return withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri)
                val input = resolver.openInputStream(uri)
                val bytes = input?.use { it.readBytes() }
                ImageData(bytes, mimeType)
            } catch (_: Throwable) {
                ImageData(null, null)
            }
        }
    }

    companion object {
        private const val ARG_USER_ID = "arg_user_id"
        private const val ARG_USER_EMAIL = "arg_user_email"
        private const val ARG_USER_NAME = "arg_user_name"
        private const val ARG_USER_AVATAR = "arg_user_avatar"
        fun newInstance(user: UserData): SettingsFragment {
            val f = SettingsFragment()
            val b = android.os.Bundle()
            b.putString(ARG_USER_ID, user.id)
            b.putString(ARG_USER_EMAIL, user.email)
            b.putString(ARG_USER_NAME, user.name)
            b.putString(ARG_USER_AVATAR, user.avatar)
            f.arguments = b
            return f
        }
    }
}
