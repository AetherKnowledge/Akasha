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
            val menu = PopupMenu(view.context, view)
            menu.menuInflater.inflate(R.menu.user_menu, menu.menu)
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

        // For now, show initial unless you wire in an image loader for ImageView
        initial.text = firstLetter
        avatar.visibility = View.GONE
        initial.visibility = View.VISIBLE
    }
}
