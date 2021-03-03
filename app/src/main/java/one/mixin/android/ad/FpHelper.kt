package one.mixin.android.ad

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Created by cc on 2021/3/3.
 */
@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
class FpHelper(val context: Context) {

    val webView: WebView by lazy {
        WebView(context).apply {
            settings.apply {
                defaultTextEncodingName = "utf-8"
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(AdHttpHelper, "MixinContext")
        }
    }


    fun loadData() {
        webView.webViewClient = object : WebViewClient() {}
        webView.clearCache(true)
        webView.loadUrl("file:///android_asset/fp.html")
    }
}