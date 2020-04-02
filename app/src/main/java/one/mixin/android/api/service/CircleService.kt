package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CircleService {
    @GET("circles")
    fun getCircles(): Call<MixinResponse<List<Circle>>>

    @POST("circles")
    suspend fun createCircle(@Body body: CircleBody): MixinResponse<Circle>

    @POST("/circles/{id}")
    suspend fun updateCircle(@Path("id") id: String, @Body body: CircleBody): MixinResponse<Circle>

    @POST("/circles/{id}/delete")
    suspend fun deleteCircle(@Path("id") id: String): MixinResponse<Any>

    @POST("/circles/{id}/conversations")
    fun getCircle(@Path("id") id: String): MixinResponse<Any>
}
