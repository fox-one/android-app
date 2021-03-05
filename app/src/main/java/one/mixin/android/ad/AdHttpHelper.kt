package one.mixin.android.ad

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import one.mixin.android.widget.ad.AdView
import org.json.JSONObject


/**
 * Created by cc on 2021/3/3.
 */


object AdHttpHelper {

    const val BASE_URL = "https://xuexi-courses-api.firesbox.com/v1/track/"
    lateinit var scope: CoroutineScope
    lateinit var context: Context
    lateinit var adView: AdView
    lateinit var activity: FragmentActivity
    fun init(scope: CoroutineScope, context: Context, activity: FragmentActivity) {
        this.scope = scope
        this.context = context
        this.activity = activity
    }

    private val httpclient by lazy {
        OkHttpClient()
    }

    fun getAdInfo(ad: AdView) {
        this.adView = ad
        FpHelper(context).loadData()
    }

    private fun getAd(id: String): String? {

        val request: Request = Request.Builder()
                // TODO: 2021/3/3 change  533b88faa59a941fb6f27f2d33a1d13f 无数据返回
//                .url(BASE_URL + id)
                .url("https://xuexi-courses-api.firesbox.com/v1/track/979d5213e70bf63093824c2f531db88f")
                .build()

        val response = httpclient.newCall(request).execute()
        return response.body?.string()
    }

    @JavascriptInterface
    fun postMessage(id: String) {
        Log.e("cxd", id)
        scope.launch(Dispatchers.IO) {
            val content = getAd(id)
            launch(Dispatchers.Main) {
                content?.let {
                    try {
                        val jsonObject = JSONObject(content)
                        val code = jsonObject.optInt("code", 0)
                        val groupName = jsonObject.optString("group_name")
                        val groupId = jsonObject.optString("group_id")
                        val groupAppId = jsonObject.optString("group_app_id")
                        val groupDesc = jsonObject.optString("group_desc")
                        val groupIcon = jsonObject.optString("group_icon")
                        val inviterName = jsonObject.optString("inviter_name")
                        val inviterId = jsonObject.optString("inviter_id")
                        val membersCount = jsonObject.optString("members_count")
                        val avatarUrl = jsonObject.optString("avatar_url")
                        val invitationCode = jsonObject.optString("invitation_code")
                        if (code != 100400) {
                            val data = AdBean(groupName, groupId, groupAppId, groupDesc, groupIcon, inviterName, inviterId, membersCount, avatarUrl, invitationCode)
                            val adDialog = AdDialogFragment()
                            adDialog.showNow(activity.supportFragmentManager, "adDialog")
                            adDialog.bindData(data)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
        }
    }
}