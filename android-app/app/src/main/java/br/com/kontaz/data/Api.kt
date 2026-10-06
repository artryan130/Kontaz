package br.com.kontaz.data

import com.google.gson.annotations.SerializedName
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class Credentials(val email: String, val password: String)
data class SignupRequest(val email: String, val password: String, val fullName: String)
data class RecoveryRequest(val email: String)
data class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)
data class AuthSession(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Long?
)

data class Dashboard(
    val period: String,
    val income: Double,
    val expenses: Double,
    val investments: Double,
    val balance: Double
)

data class Transaction(
    val id: String,
    val amount: Double,
    val type: String,
    val category: String,
    val description: String?,
    val date: String
)

data class TransactionList(val items: List<Transaction>, val limit: Int, val offset: Int)
data class TransactionWrite(
    val amount: Double,
    val type: String,
    val category: String,
    val description: String?,
    val date: String
)

interface KontazApi {
    @POST("v1/auth/login")
    suspend fun login(@Body body: Credentials): AuthSession

    @POST("v1/auth/signup")
    suspend fun signup(@Body body: SignupRequest): AuthSession

    @POST("v1/auth/recover")
    suspend fun recover(@Body body: RecoveryRequest)

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthSession

    @GET("v1/dashboard")
    suspend fun dashboard(@Query("period") period: String): Dashboard

    @GET("v1/transactions")
    suspend fun transactions(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): TransactionList

    @POST("v1/transactions")
    suspend fun createTransaction(@Body body: TransactionWrite): Transaction

    @PATCH("v1/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: String, @Body body: Map<String, Any?>): Transaction

    @DELETE("v1/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String)
}

class ApiFactory(
    baseUrl: String,
    private val sessionStore: SessionStore
) {
    private val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val api: KontazApi = Retrofit.Builder()
        .baseUrl(normalizedBaseUrl)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    val request = chain.request()
                    val token = sessionStore.accessToken()
                    val authenticated = if (token != null && !request.url.encodedPath.endsWith("/auth/login") &&
                        !request.url.encodedPath.endsWith("/auth/signup") &&
                        !request.url.encodedPath.endsWith("/auth/recover") &&
                        !request.url.encodedPath.endsWith("/auth/refresh")
                    ) {
                        request.newBuilder().header("Authorization", "Bearer $token").build()
                    } else request
                    chain.proceed(authenticated)
                })
                .authenticator(object : Authenticator {
                    override fun authenticate(route: Route?, response: Response): Request? {
                        val path = response.request.url.encodedPath
                        if (path.contains("/v1/auth/") || responseCount(response) >= 2) return null

                        val previousToken = response.request.header("Authorization")
                            ?.removePrefix("Bearer ")
                            ?: return null
                        val refreshToken = sessionStore.refreshToken() ?: return null

                        synchronized(sessionStore) {
                            val currentToken = sessionStore.accessToken()
                            if (currentToken != null && currentToken != previousToken) {
                                return response.request.newBuilder()
                                    .header("Authorization", "Bearer $currentToken")
                                    .build()
                            }

                            val refreshBody = Gson().toJson(RefreshRequest(refreshToken))
                                .toRequestBody(jsonMediaType)
                            val refreshRequest = Request.Builder()
                                .url("${normalizedBaseUrl}v1/auth/refresh")
                                .post(refreshBody)
                                .build()
                            val refreshResponse = OkHttpClient().newCall(refreshRequest).execute()
                            refreshResponse.use { result ->
                                if (!result.isSuccessful) {
                                    if (result.code == 400 || result.code == 401) sessionStore.clear()
                                    return null
                                }
                                val session = result.body?.charStream()?.use {
                                    Gson().fromJson(it, AuthSession::class.java)
                                } ?: return null
                                val newToken = session.accessToken ?: return null
                                sessionStore.save(session)
                                return response.request.newBuilder()
                                    .header("Authorization", "Bearer $newToken")
                                    .build()
                            }
                        }
                    }
                })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
        .build()
        .create(KontazApi::class.java)

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
