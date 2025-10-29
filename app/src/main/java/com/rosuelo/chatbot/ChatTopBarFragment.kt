package com.rosuelo.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class ChatTopBarFragment : Fragment() {

    interface Listener {
        fun onHamburgerClick() {}
        fun onSettingsClick() {}
        fun onLogoutClick() {}
    }

    var listener: Listener? = null

    private var imgAvatar: ImageView? = null
    private var txtInitial: TextView? = null
    private var avatarContainer: FrameLayout? = null
    private var hamburger: ImageButton? = null

    private var cachedUser: UserData? = null
    private var lastAvatarUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.chat_top_bar, container, false)
        // Transparent background; pad for status bar insets to avoid punch-hole overlap
        val initialTop = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = initialTop + topInset)
            insets
        }
        imgAvatar = root.findViewById(R.id.imgAvatar)
        txtInitial = root.findViewById(R.id.txtAvatarInitial)
        avatarContainer = root.findViewById(R.id.userAvatarContainer)
        hamburger = root.findViewById(R.id.btnHamburger)

        hamburger?.setOnClickListener { listener?.onHamburgerClick() }

        avatarContainer?.setOnClickListener { view ->
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(view.context, R.style.ThemeOverlay_Chatbot_PopupMenu)
            val menu = PopupMenu(themedContext, view)
            menu.menuInflater.inflate(R.menu.user_menu, menu.menu)
            // Force white text on items for consistency
            try {
                val field = PopupMenu::class.java.getDeclaredField("mPopup")
                field.isAccessible = true
                val helper = field.get(menu)
                val method = helper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(helper, true)
            } catch (_: Throwable) {}
            menu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_settings -> { listener?.onSettingsClick(); true }
                    R.id.action_logout -> { listener?.onLogoutClick(); true }
                    else -> false
                }
            }
            menu.show()
        }

        cachedUser?.let { bindUser(it) }

        return root
    }

    fun bindUser(user: UserData) {
        cachedUser = user
        val avatar = imgAvatar
        val initial = txtInitial
        if (avatar == null || initial == null) return

        val firstLetter = user.name?.firstOrNull()?.uppercaseChar()?.toString()
            ?: user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        val avatarUrl = user.avatar
        if (!avatarUrl.isNullOrBlank()) {
            // Avoid re-downloading same URL
            if (lastAvatarUrl == avatarUrl && avatar.drawable != null) {
                initial.visibility = View.GONE
                avatar.visibility = View.VISIBLE
                return
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    URL(avatarUrl).openStream().use { input ->
                        val bmp = BitmapFactory.decodeStream(input)
                        if (bmp != null && isAdded) {
                            launch(Dispatchers.Main) {
                                avatar.setImageBitmap(bmp)
                                initial.visibility = View.GONE
                                avatar.visibility = View.VISIBLE
                                lastAvatarUrl = avatarUrl
                            }
                        } else {
                            launch(Dispatchers.Main) {
                                initial.text = firstLetter
                                avatar.visibility = View.GONE
                                initial.visibility = View.VISIBLE
                                lastAvatarUrl = null
                            }
                        }
                    }
                } catch (_: Throwable) {
                    launch(Dispatchers.Main) {
                        initial.text = firstLetter
                        avatar.visibility = View.GONE
                        initial.visibility = View.VISIBLE
                        lastAvatarUrl = null
                    }
                }
            }
        } else {
            initial.text = firstLetter
            avatar.visibility = View.GONE
            initial.visibility = View.VISIBLE
            lastAvatarUrl = null
        }
    }
}
