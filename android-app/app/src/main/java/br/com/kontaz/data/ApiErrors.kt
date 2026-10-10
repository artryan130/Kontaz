package br.com.kontaz.data

import com.google.gson.JsonParser
import java.io.IOException
import retrofit2.HttpException

internal fun userFacingError(
    error: Throwable,
    fallback: String,
    unauthorizedMessage: String? = null
): String {
    if (error is IOException) {
        return "Não foi possível conectar ao serviço. Verifique sua internet e tente novamente."
    }

    if (error !is HttpException) return error.message?.takeIf(String::isNotBlank) ?: fallback

    val apiCode = runCatching {
        error.response()?.errorBody()?.string()
            ?.let(JsonParser::parseString)
            ?.asJsonObject
            ?.getAsJsonObject("error")
            ?.get("code")
            ?.asString
    }.getOrNull()

    return when (apiCode) {
        "INVALID_CREDENTIALS" -> "E-mail ou senha incorretos. Confira seus dados e tente novamente."
        "UNAUTHORIZED" -> unauthorizedMessage ?: "Sua sessão expirou. Entre novamente para continuar."
        "INVALID_INPUT" -> "Confira os dados informados e tente novamente."
        "SIGNUP_FAILED" -> "Não foi possível criar a conta. Verifique se o e-mail já está cadastrado e se a senha tem pelo menos 6 caracteres."
        "INVALID_PASSWORD" -> "A senha precisa ter entre 6 e 128 caracteres."
        "RATE_LIMITED" -> "Muitas tentativas. Aguarde um pouco e tente novamente."
        "NOT_FOUND" -> "O item solicitado não foi encontrado. Atualize a tela e tente novamente."
        "DATABASE_ERROR", "DATA_ERROR" -> "Não foi possível concluir a operação. Tente novamente."
        "AUTH_PROVIDER_ERROR", "AUTH_PROVIDER_UNAVAILABLE", "RECOVERY_FAILED" ->
            "O serviço está temporariamente indisponível. Tente novamente mais tarde."
        else -> when (error.code()) {
            400 -> "Confira os dados informados e tente novamente."
            401 -> unauthorizedMessage ?: "Sua sessão expirou. Entre novamente para continuar."
            404 -> "O recurso solicitado não foi encontrado."
            429 -> "Muitas tentativas. Aguarde um pouco e tente novamente."
            in 500..599 -> "O serviço está temporariamente indisponível. Tente novamente mais tarde."
            else -> fallback
        }
    }
}
