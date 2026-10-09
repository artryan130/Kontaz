package br.com.kontaz.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class OAuthRequest(val authorizationUrl: String, val verifier: String)

class SupabaseOAuth(
    private val supabaseUrl: String,
    private val anonKey: String
) {
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    fun createGoogleRequest(): OAuthRequest {
        val verifier = randomUrlSafeValue(64)
        val challenge = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val url = "$supabaseUrl/auth/v1/authorize".toHttpUrl().newBuilder()
            .addQueryParameter("provider", "google")
            .addQueryParameter("redirect_to", REDIRECT_URI)
            .addQueryParameter("code_challenge", challenge)
            .addQueryParameter("code_challenge_method", "s256")
            .build()
        return OAuthRequest(url.toString(), verifier)
    }

    suspend fun exchangeCode(code: String, verifier: String): AuthSession = withContext(Dispatchers.IO) {
        val requestBody = gson.toJson(
            mapOf("auth_code" to code, "code_verifier" to verifier)
        ).toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=pkce")
            .header("apikey", anonKey)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) {
                throw IllegalStateException("Não foi possível concluir o login com Google. Tente novamente.")
            }
            gson.fromJson(body, AuthSession::class.java)
                ?: throw IllegalStateException("O serviço de autenticação retornou uma resposta inválida.")
        }
    }

    private fun randomUrlSafeValue(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        const val REDIRECT_URI = "kontaz://auth/callback"
    }
}
