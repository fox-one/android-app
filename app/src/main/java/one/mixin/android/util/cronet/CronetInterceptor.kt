package one.mixin.android.util.cronet

import android.content.ContentResolver
import com.google.gson.JsonSyntaxException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.ExpiredTokenException
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.di.AppModule
import one.mixin.android.di.HostSelectionInterceptor
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.show
import one.mixin.android.session.JwtResult
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException
import org.chromium.net.CronetEngine
import org.chromium.net.UploadDataProviders
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ExecutorService
import kotlin.math.abs

class CronetInterceptor(
    private val resolver: ContentResolver,
    private val cronetEngine: CronetEngine,
    private val executorService: ExecutorService
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sourceRequest = chain.request()
        val request = sourceRequest.newBuilder()
            .addHeader("User-Agent", AppModule.API_UA)
            .addHeader("Accept-Language", Locale.getDefault().language)
            .addHeader("Mixin-Device-Id", AppModule.getDeviceId(resolver))
            .addHeader("Authorization", "Bearer ${Session.signToken(Session.getAccount(), sourceRequest)}")
            .build()
        if (MixinApplication.appContext.networkConnected()) {
            var response = try {
                if (request.header("Upgrade") == "websocket") {
                    return chain.proceed(request)
                } else {
                    proceedWithCronet(chain, request)
                }
            } catch (e: Exception) {
                throw e.apply {
                    if (this is SocketTimeoutException || this is UnknownHostException || this is ConnectException) {
                        HostSelectionInterceptor.get().switch(request)
                    }
                }
            }

            if (!response.isSuccessful) {
                val code = response.code
                if (code in 501..599) {
                    HostSelectionInterceptor.get().switch(request)
                    throw ServerErrorException(code)
                } else if (code == 500) {
                    throw ServerErrorException(code)
                }
            }

            var jwtResult: JwtResult? = null
            response.body?.run {
                val bytes = this.bytes()
                val contentType = this.contentType()
                val body = bytes.toResponseBody(contentType)
                response = response.newBuilder().body(body).build()
                if (bytes.isEmpty()) return@run
                val mixinResponse = try {
                    GsonHelper.customGson.fromJson(String(bytes), MixinResponse::class.java)
                } catch (e: JsonSyntaxException) {
                    HostSelectionInterceptor.get().switch(request)
                    throw ServerErrorException(response.code)
                }
                if (mixinResponse.errorCode == ErrorHandler.OLD_VERSION) {
                    MixinApplication.get().gotoOldVersionAlert()
                    return@run
                } else if (mixinResponse.errorCode != ErrorHandler.AUTHENTICATION) return@run
                val authorization = response.request.header("Authorization")
                if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ")) {
                    val jwt = authorization.substring(7)
                    jwtResult = Session.requestDelay(Session.getAccount(), jwt, Constants.DELAY_SECOND)
                    if (jwtResult?.isExpire == true) {
                        throw ExpiredTokenException()
                    }
                }
            }

            if (MixinApplication.get().onlining.get()) {
                response.header("X-Server-Time")?.toLong()?.let { serverTime ->
                    val currentTime = System.currentTimeMillis()
                    if (abs(serverTime / 1000000 - System.currentTimeMillis()) >= Constants.ALLOW_INTERVAL) {
                        MixinApplication.get().gotoTimeWrong(serverTime)
                    } else if (jwtResult?.isExpire == false) {
                        jwtResult?.serverTime = serverTime / 1000000000
                        jwtResult?.currentTime = currentTime / 1000
                        val ise = IllegalStateException("Force logout. $jwtResult. request: ${request.show()}, response: ${response.show()}")
                        reportException(ise)
                        MixinApplication.get().closeAndClear()
                    }
                }
            }

            return response
        } else {
            throw NetworkException()
        }
    }

    private fun proceedWithCronet(
        chain: Interceptor.Chain,
        request: Request
    ): Response {
        val call = chain.call()
        val callback = CronetCallback(request, call)

        val urlRequest = cronetEngine.newUrlRequestBuilder(request.url.toString(), callback, executorService).apply {
            allowDirectExecutor()
            setHttpMethod(request.method)

            val headers = request.headers
            for (i in 0 until headers.size) {
                addHeader(headers.name(i), headers.value(i))
            }

            request.body?.let {
                addHeader("Content-Type", it.contentType().toString())

                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val dataProvider = UploadDataProviders.create(buffer.readByteArray())
                setUploadDataProvider(dataProvider, executorService)
            }
        }.build()

        urlRequest.start()

        return callback.waitDone()
    }
}
