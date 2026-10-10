package br.com.kontaz.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.kontaz.data.Transaction
import br.com.kontaz.data.TransactionWrite
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TransactionTypeStyle(
    val key: String,
    val title: String,
    val subtitle: String,
    val color: Color,
    val softColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val transactionTypes = listOf(
    TransactionTypeStyle(
        "income", "Receita", "Adicionar receita", KontazColors.Green,
        Color(0xFFE0F4EA), Icons.Outlined.ArrowUpward
    ),
    TransactionTypeStyle(
        "expense", "Despesa", "Adicionar despesa", KontazColors.Orange,
        Color(0xFFF8EAE0), Icons.Outlined.ArrowDownward
    ),
    TransactionTypeStyle(
        "investment", "Investimento", "Adicionar investimento", KontazColors.Blue,
        Color(0xFFE0F1F8), Icons.Outlined.Savings
    )
)

@Composable
fun TransactionDialog(
    transaction: Transaction?,
    onDismiss: () -> Unit,
    onSave: (TransactionWrite) -> Unit
) {
    var selectedType by remember(transaction?.id) { mutableStateOf(transaction?.type) }
    if (selectedType == null) {
        TransactionTypePicker(onDismiss = onDismiss, onSelect = { selectedType = it })
    } else {
        TransactionEntryForm(
            transaction = transaction,
            type = selectedType!!,
            onBack = {
                if (transaction == null) selectedType = null else onDismiss()
            },
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@Composable
private fun TransactionTypePicker(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = KontazColors.Background
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Nova Transação",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        color = KontazColors.Text,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fechar", tint = KontazColors.Muted)
                    }
                }
                Text("Escolha o tipo de transação", color = KontazColors.Muted)
                transactionTypes.forEach { style ->
                    Surface(
                        color = style.softColor,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(style.key) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 19.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(28.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(style.title, color = style.color, fontWeight = FontWeight.SemiBold)
                                Text(style.subtitle, color = style.color, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionEntryForm(
    transaction: Transaction?,
    type: String,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (TransactionWrite) -> Unit
) {
    val context = LocalContext.current
    val dismissKeyboardActions = rememberDismissKeyboardActions()
    val style = transactionTypes.first { it.key == type }
    var amount by remember(transaction?.id) { mutableStateOf(transaction?.amount?.let(::formatInputAmount).orEmpty()) }
    var category by remember(transaction?.id) { mutableStateOf(transaction?.category.orEmpty()) }
    var description by remember(transaction?.id) { mutableStateOf(transaction?.description.orEmpty()) }
    var date by remember(transaction?.id) { mutableStateOf(transaction?.date ?: LocalDate.now().toString()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val categories = categoriesFor(type)

    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable {}.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = KontazColors.Background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = KontazColors.Text)
                    }
                    Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(20.dp))
                    Text(
                        if (transaction == null) "Nova ${style.title}" else "Editar ${style.title}",
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = KontazColors.Text
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fechar", tint = KontazColors.Muted)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                FormLabel("Valor (R$)", required = true)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = normalizeDecimalInput(value) },
                    modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
                    placeholder = { Text("R$ 0,00", color = KontazColors.Muted.copy(alpha = 0.65f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = dismissKeyboardActions,
                    visualTransformation = BrazilianNumberVisualTransformation(),
                    supportingText = { Text("Os dois últimos dígitos são os centavos.") },
                    shape = RoundedCornerShape(15.dp)
                )

                FormLabel("Categoria", required = true)
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { categoryMenuExpanded = true },
                        readOnly = true,
                        placeholder = { Text("Selecione uma categoria") },
                        trailingIcon = {
                            Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = KontazColors.Muted)
                        },
                        shape = RoundedCornerShape(15.dp)
                    )
                    Box(Modifier.matchParentSize().clickable { categoryMenuExpanded = true })
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier.background(KontazColors.Surface)
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    category = item
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                FormLabel("Data")
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        val initialDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                date = LocalDate.of(year, month + 1, day).toString()
                            },
                            initialDate.year,
                            initialDate.monthValue - 1,
                            initialDate.dayOfMonth
                        ).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                            .background(KontazColors.Surface, RoundedCornerShape(15.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(date.toDisplayDate(), modifier = Modifier.weight(1f), color = KontazColors.Text)
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Selecionar data", tint = KontazColors.Muted)
                    }
                }

                FormLabel("Descrição (opcional)")
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
                    placeholder = { Text("Ex: ${descriptionHint(type)}", color = KontazColors.Muted.copy(alpha = 0.65f)) },
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, tint = KontazColors.Muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = dismissKeyboardActions,
                    shape = RoundedCornerShape(15.dp)
                )

                validationMessage?.let { Text(it, color = KontazColors.Red, style = MaterialTheme.typography.bodySmall) }
                }
                Button(
                    onClick = {
                        val parsedAmount = amount.toDecimalOrNull()
                        if (parsedAmount == null || parsedAmount <= 0 || !isValidDate(date) || category.isBlank()) {
                            validationMessage = "Informe um valor válido e selecione uma categoria."
                        } else {
                            onSave(TransactionWrite(parsedAmount, type, category, description.trim().ifBlank { null }, date))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = style.color)
                ) {
                    Text(if (transaction == null) "Adicionar ${style.title}" else "Salvar alterações", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FormLabel(text: String, required: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = KontazColors.Muted, style = MaterialTheme.typography.bodySmall)
        if (required) Text(" *", color = KontazColors.Orange, style = MaterialTheme.typography.bodySmall)
    }
}

private fun categoriesFor(type: String): List<String> = when (type) {
    "income" -> listOf("Salário", "Freelance", "Investimentos", "Vendas", "Benefícios", "Outros")
    "investment" -> listOf("Renda Variável", "Renda Fixa", "Fundos", "Criptomoedas", "Previdência", "Outros")
    else -> listOf("Alimentação", "Moradia", "Transporte", "Saúde", "Educação", "Lazer", "Contas", "Outros")
}

private fun descriptionHint(type: String): String = when (type) {
    "income" -> "Salário do mês"
    "investment" -> "Aplicação em renda variável"
    else -> "Compra no mercado"
}

private fun formatInputAmount(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun String.toDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(this)

private fun isValidDate(value: String): Boolean = runCatching {
    LocalDate.parse(value)
}.isSuccess
