package br.com.kontaz.data

import java.text.Normalizer
import java.util.Locale

data class TransactionCategory(
    val id: String,
    val type: String,
    val name: String,
    @com.google.gson.annotations.SerializedName("is_custom") val isCustom: Boolean
)

data class TransactionCategoryList(val items: List<TransactionCategory>)

data class TransactionCategoryWrite(val type: String, val name: String)

fun builtInCategoryNames(type: String): List<String> = when (type) {
    "income" -> listOf("Salário", "Freelance", "Investimentos", "Vendas", "Benefícios", "Outros")
    "investment" -> listOf("Renda Variável", "Renda Fixa", "Fundos", "Criptomoedas", "Previdência", "Outros")
    else -> listOf("Alimentação", "Moradia", "Transporte", "Saúde", "Educação", "Lazer", "Contas", "Outros")
}

fun normalizedCategoryKey(name: String): String =
    Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

fun builtInCategoryName(type: String, key: String): String? =
    builtInCategoryNames(type).firstOrNull { normalizedCategoryKey(it) == key }
