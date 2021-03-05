package one.mixin.android.widget.ad

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.ad.AdBean
import one.mixin.android.extension.loadImage
import one.mixin.android.widget.CircleImageView

/**
 * Created by cc on 2021/3/3.
 */
class AdView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {


    init {
        View.inflate(context, R.layout.view_ad_home, this)
    }

    fun bindData(data: AdBean) {
        findViewById<TextView>(R.id.tv_inviter_name).text = data.inviter_name
        findViewById<TextView>(R.id.tv_inviter_id).text = data.inviter_id
        findViewById<TextView>(R.id.tv_group_name).text = data.group_name
        // TODO: 2021/3/3 change
        findViewById<TextView>(R.id.tv_group_owner).text = data.group_id
        findViewById<TextView>(R.id.tv_group_desc).text = data.group_desc
        findViewById<TextView>(R.id.tv_group_number).text = data.members_count + "人已入群"
        findViewById<TextView>(R.id.tv_inviter_name).text = data.inviter_name

        findViewById<CircleImageView>(R.id.v_inviter).loadImage(data.avatar_url, R.drawable.ic_avatar_place_holder)
        findViewById<CircleImageView>(R.id.v_group_avatar).loadImage(data.group_icon, R.drawable.ic_avatar_place_holder)

        findViewById<TextView>(R.id.tv_join).setOnClickListener {
            val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("mixin://apps/${data.group_app_id}/"))
            context.startActivity(intent)
        }
    }
}