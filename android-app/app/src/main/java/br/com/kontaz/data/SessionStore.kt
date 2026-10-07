package br.com.kontaz.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "kontaz_session",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun accessToken(): String? = preferences.getString(ACCESS_TOKEN, null)
    fun refreshToken(): String? = preferences.getString(REFRESH_TOKEN, null)
    fun displayName(): String? = preferences.getString(DISPLAY_NAME, null)

    fun save(session: AuthSession) {
        val accessToken = requireNotNull(session.accessToken) { "Authentication response did not include an access token." }
        val refreshToken = requireNotNull(session.refreshToken) { "Authentication response did not include a refresh token." }
        preferences.edit()
            .putString(ACCESS_TOKEN, accessToken)
            .putString(REFRESH_TOKEN, refreshToken)
            .putString(DISPLAY_NAME, session.user?.userMetadata?.get("full_name"))
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val DISPLAY_NAME = "display_name"
    }
}
