package eu.mcomputing.mobv.mobvzadanie.data.api

import android.content.Context
import eu.mcomputing.mobv.mobvzadanie.data.api.helper.AuthInterceptor
import eu.mcomputing.mobv.mobvzadanie.data.api.helper.TokenAuthenticator
import eu.mcomputing.mobv.mobvzadanie.data.api.model.ChangePasswordRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.ChangePasswordResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.ForgottenPasswordRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.ForgottenPasswordResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.GeofenceListResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.GeofenceUpdateRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.GeofenceUpdateResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.ImageResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.LoginResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.RefreshTokenRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.RefreshTokenResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.RegistrationResponse
import eu.mcomputing.mobv.mobvzadanie.data.api.model.UserLoginRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.UserRegistrationRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @POST("user/create.php")
    suspend fun registerUser(@Body userInfo: UserRegistrationRequest): Response<RegistrationResponse>

    @POST("user/login.php")
    suspend fun loginUser(@Body userInfo: UserLoginRequest): Response<LoginResponse>

    @POST("user/password.php")
    suspend fun changePassword(@Body passwordInfo: ChangePasswordRequest): Response<ChangePasswordResponse>

    @GET("user/get.php")
    suspend fun getUser(
        @Query("id") id: String
    ): Response<UserResponse>

    @POST("user/refresh.php")
    fun refreshTokenBlocking(
        @Body refreshInfo: RefreshTokenRequest
    ): Call<RefreshTokenResponse>

    @GET("geofence/list.php")
    suspend fun listGeofence(): Response<GeofenceListResponse>

    @POST("geofence/update.php")
    suspend fun updateGeofence(@Body body: GeofenceUpdateRequest): Response<GeofenceUpdateResponse>

    @DELETE("geofence/update.php")
    suspend fun deleteGeofence(): Response<GeofenceUpdateResponse>

    @Multipart
    @POST("https://upload.mcomputing.eu/user/photo.php")
    suspend fun uploadPhoto(@Part image: MultipartBody.Part): Response<ImageResponse>

    @DELETE("https://upload.mcomputing.eu/user/photo.php")
    suspend fun deletePhoto(): Response<ImageResponse>

    @POST("user/reset.php")
    suspend fun forgottenPassword(@Body email: ForgottenPasswordRequest): Response<ForgottenPasswordResponse>

    companion object {
        fun create(context: Context): ApiService {

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context))
                .authenticator(TokenAuthenticator(context))
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://zadanie.mpage.sk/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}