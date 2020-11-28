package one.mixin.android.ui.forward

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_contact_header.view.*
import one.mixin.android.R
import one.mixin.android.databinding.ItemContactHeaderBinding
import one.mixin.android.extension.inflate
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.User
import one.mixin.android.widget.ConversationCheckView

class ForwardAdapter(private val disableCheck: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyRecyclerHeadersAdapter<ForwardAdapter.HeaderViewHolder> {

    companion object {
        const val TYPE_CONVERSATION = 0
        const val TYPE_FRIEND = 1
        const val TYPE_BOT = 2
    }

    var selectItem = ArrayList<Any>()

    private var listener: ForwardListener? = null
    var conversations: List<ConversationItem>? = null
    var friends: List<User>? = null
    var bots: List<User>? = null

    var sourceConversations: List<ConversationItem>? = null
    var sourceFriends: List<User>? = null
    var sourceBots: List<User>? = null

    var showHeader: Boolean = true
    var keyword: CharSequence? = null

    fun changeData() {
        if (!keyword.isNullOrBlank()) {
            conversations = sourceConversations?.filter {
                if (it.isGroup()) {
                    it.groupName != null && (it.groupName.contains(keyword.toString(), ignoreCase = true))
                } else {
                    it.name.contains(keyword.toString(), ignoreCase = true) ||
                        it.ownerIdentityNumber.startsWith(keyword.toString(), ignoreCase = true)
                }
            }?.sortedByDescending {
                if (it.isGroup()) {
                    it.groupName == keyword
                } else {
                    it.name == keyword || it.ownerIdentityNumber == keyword
                }
            }
            friends = sourceFriends?.filter {
                (it.fullName != null && it.fullName.contains(keyword.toString(), ignoreCase = true)) ||
                    it.identityNumber.startsWith(keyword.toString(), ignoreCase = true)
            }?.sortedByDescending {
                it.fullName == keyword || it.identityNumber == keyword
            }
            bots = sourceBots?.filter {
                (it.fullName != null && it.fullName.contains(keyword.toString(), ignoreCase = true)) ||
                    it.identityNumber.startsWith(keyword.toString(), ignoreCase = true)
            }?.sortedByDescending {
                it.fullName == keyword || it.identityNumber == keyword
            }
            showHeader = false
        } else {
            conversations = sourceConversations
            friends = sourceFriends
            bots = sourceBots
            showHeader = true
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (conversations == null && friends == null && bots == null) {
            0
        } else if (conversations == null) {
            friends?.size?.plus(bots?.size ?: 0) ?: 0
        } else if (friends == null) {
            conversations?.size?.plus(bots?.size ?: 0) ?: 0
        } else if (bots == null) {
            friends?.size?.plus(conversations?.size ?: 0) ?: 0
        } else {
            conversations!!.size + friends!!.size + bots!!.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            TYPE_CONVERSATION
        } else if (friends != null && friends!!.isNotEmpty() && position < conversations!!.size + friends!!.size) {
            TYPE_FRIEND
        } else {
            TYPE_BOT
        }
    }

    override fun getHeaderId(position: Int): Long {
        if (!showHeader) {
            return -1
        }
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            1
        } else if (friends != null && friends!!.isNotEmpty() && position < conversations!!.size + friends!!.size) {
            2
        } else {
            3
        }
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (conversations.isNullOrEmpty() && friends.isNullOrEmpty() && bots.isNullOrEmpty()) {
            return
        }
        holder.itemView.header.text = holder.itemView.context.getString(
            if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
                R.string.chat_item_title
            } else if (friends != null && friends!!.isNotEmpty() && position < conversations!!.size + friends!!.size) {
                R.string.contact_item_title
            } else {
                R.string.bot_item_title
            }
        )
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(ItemContactHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (conversations.isNullOrEmpty() && friends.isNullOrEmpty() && bots.isNullOrEmpty()) {
            return
        }
        when (holder) {
            is ConversationViewHolder -> {
                val conversationItem = conversations!![position]
                holder.bind(conversationItem, listener, selectItem.contains(conversationItem))
            }
            is FriendViewHolder -> {
                val pos = position - (conversations?.size ?: 0)
                val user = friends!![pos]
                holder.bind(user, listener, selectItem.contains(user))
            }
            else -> {
                holder as BotViewHolder
                val pos = position - (conversations?.size ?: 0) - (friends?.size ?: 0)
                val user = bots!![pos]
                holder.bind(user, listener, selectItem.contains(user))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CONVERSATION -> {
                ConversationViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_forward_conversation,
                        parent,
                        false
                    ).apply {
                        if (disableCheck) {
                            (this as ConversationCheckView).disableCheck()
                        }
                    }
                )
            }
            TYPE_FRIEND -> {
                FriendViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_contact_friend,
                        parent,
                        false
                    ).apply {
                        if (disableCheck) {
                            (this as ConversationCheckView).disableCheck()
                        }
                    }
                )
            }
            else -> {
                BotViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_contact_friend,
                        parent,
                        false
                    ).apply {
                        if (disableCheck) {
                            (this as ConversationCheckView).disableCheck()
                        }
                    }
                )
            }
        }
    }

    fun setForwardListener(listener: ForwardListener) {
        this.listener = listener
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: User, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: User, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ConversationItem, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class HeaderViewHolder(binding: ItemContactHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    interface ForwardListener {
        fun onUserItemClick(user: User)
        fun onConversationItemClick(item: ConversationItem)
    }
}
