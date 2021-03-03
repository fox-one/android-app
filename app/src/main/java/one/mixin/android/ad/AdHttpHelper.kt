package one.mixin.android.ad

import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import com.google.gson.JsonObject
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
    fun init(scope: CoroutineScope, context: Context) {
        this.scope = scope
        this.context = context
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
                // TODO: 2021/3/3 change
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
                            adView.bindData(data)
                            adView.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {

                    }

                }
            }
        }
    }
}