package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.vo.Address
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by cc on 2021/3/3.
 */
interface AdService {

    @POST("addresses")
    suspend fun addresses(@Body request: AddressRequest): MixinResponse<Address>
}