package br.com.kontaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.kontaz.data.ApiFactory
import br.com.kontaz.data.Credentials
import br.com.kontaz.data.Dashboard
import br.com.kontaz.data.RecoveryRequest
import br.com.kontaz.data.SessionStore
import br.com.kontaz.data.SignupRequest
import br.com.kontaz.data.Transaction
import br.com.kontaz.data.TransactionWrite
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                KontazApp()
            }
        }
    }
}

@Composable
private fun KontazApp() {
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    val api = remember { ApiFactory(BuildConfig.API_BASE_URL, sessionStore).api }
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(sessionStore.accessToken() != null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var dashboard by remember { mutableStateOf<Dashboard?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("dashboard") }
    var fromFilter by remember { mutableStateOf("") }
    var toFilter by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("") }
    var showTransactionForm by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    fun loadHome(from: String? = null, to: String? = null, type: String? = null) {
        scope.launch {
            loading = true
            message = null
            try {
                val period = YearMonth.now().toString()
                dashboard = api.dashboard(period)
                transactions = api.transactions(from = from, to = to, type = type, limit = 50).items
            } catch (error: Exception) {
                message = error.message ?: "Não foi possível carregar seus dados."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(signedIn) {
        if (signedIn) loadHome()
    }

    if (!signedIn) {
        LoginScreen(
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            loading = loading,
            message = message,
            onLogin = {
                scope.launch {
                    loading = true
                    message = null
                    try {
                        sessionStore.save(api.login(Credentials(email.trim(), password)))
                        signedIn = true
                    } catch (error: Exception) {
                        message = error.message ?: "Não foi possível entrar."
                    } finally {
                        loading = false
                    }
                }
            },
            onSignup = { fullName ->
                scope.launch {
                    loading = true
                    message = null
                    try {
                        val session = api.signup(SignupRequest(email.trim(), password, fullName.trim()))
                        if (!session.accessToken.isNullOrBlank() && !session.refreshToken.isNullOrBlank()) {
                            sessionStore.save(session)
                            signedIn = true
                        } else {
                            message = "Conta criada. Confirme seu e-mail e entre para continuar."
                        }
                    } catch (error: Exception) {
                        message = error.message ?: "Não foi possível criar a conta."
                    } finally {
                        loading = false
                    }
                }
            },
            onRecover = {
                scope.launch {
                    loading = true
                    message = null
                    try {
                        api.recover(RecoveryRequest(email.trim()))
                        message = "Se a conta existir, enviaremos instruções para recuperar o acesso."
                    } catch (error: Exception) {
                        message = error.message ?: "Não foi possível solicitar a recuperação."
                    } finally {
                        loading = false
                    }
                }
            }
        )
    } else {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selectedTab = "dashboard"; loadHome() }) { Text("Resumo") }
                    TextButton(onClick = { selectedTab = "history" }) { Text("Histórico") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        sessionStore.clear()
                        signedIn = false
                        dashboard = null
                        transactions = emptyList()
                    }) { Text("Sair") }
                }
                Text(if (selectedTab == "dashboard") "Resumo do mês" else "Histórico", style = MaterialTheme.typography.headlineSmall)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (loading && dashboard == null) CircularProgressIndicator()
                if (selectedTab == "dashboard") {
                    dashboard?.let { DashboardCards(it) }
                    Text("Transações recentes", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = {
                        editingTransaction = null
                        showTransactionForm = true
                    }) { Text("Nova transação") }
                    TransactionList(
                        transactions.take(5),
                        onEdit = { editingTransaction = it; showTransactionForm = true },
                        onDelete = { transactionToDelete = it }
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromFilter,
                            onValueChange = { fromFilter = it },
                            label = { Text("De (AAAA-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toFilter,
                            onValueChange = { toFilter = it },
                            label = { Text("Até (AAAA-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = typeFilter,
                        onValueChange = { typeFilter = it },
                        label = { Text("Tipo (income/expense/investment, opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        loadHome(
                            from = fromFilter.takeIf(String::isNotBlank),
                            to = toFilter.takeIf(String::isNotBlank),
                            type = typeFilter.trim().takeIf(String::isNotBlank)
                        )
                    }) { Text("Filtrar histórico") }
                    Button(onClick = {
                        editingTransaction = null
                        showTransactionForm = true
                    }) { Text("Nova transação") }
                    TransactionList(
                        transactions,
                        onEdit = { editingTransaction = it; showTransactionForm = true },
                        onDelete = { transactionToDelete = it }
                    )
                }
                if (transactions.isEmpty() && !loading && message == null) {
                    Text("Ainda não há transações neste período.")
                }
                TextButton(onClick = {
                    loadHome(
                        from = if (selectedTab == "history") fromFilter.takeIf(String::isNotBlank) else null,
                        to = if (selectedTab == "history") toFilter.takeIf(String::isNotBlank) else null,
                        type = if (selectedTab == "history") typeFilter.trim().takeIf(String::isNotBlank) else null
                    )
                }) { Text("Atualizar") }
            }
        }
    }

    if (showTransactionForm) {
        TransactionFormDialog(
            transaction = editingTransaction,
            onDismiss = { showTransactionForm = false },
            onSave = { body ->
                scope.launch {
                    loading = true
                    message = null
                    try {
                        val current = editingTransaction
                        if (current == null) api.createTransaction(body)
                        else api.updateTransaction(
                            current.id,
                            mapOf(
                                "amount" to body.amount,
                                "type" to body.type,
                                "category" to body.category,
                                "description" to body.description,
                                "date" to body.date
                            )
                        )
                        showTransactionForm = false
                        loadHome(
                            from = fromFilter.takeIf(String::isNotBlank),
                            to = toFilter.takeIf(String::isNotBlank),
                            type = typeFilter.trim().takeIf(String::isNotBlank)
                        )
                    } catch (error: Exception) {
                        message = error.message ?: "Não foi possível salvar a transação."
                    } finally {
                        loading = false
                    }
                }
            }
        )
    }

    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Excluir transação?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        loading = true
                        try {
                            api.deleteTransaction(transaction.id)
                            transactionToDelete = null
                            loadHome()
                        } catch (error: Exception) {
                            message = error.message ?: "Não foi possível excluir a transação."
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun LoginScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    loading: Boolean,
    message: String?,
    onLogin: () -> Unit,
    onSignup: (String) -> Unit,
    onRecover: () -> Unit
) {
    var mode by remember { mutableStateOf("login") }
    var fullName by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Kontaz", style = MaterialTheme.typography.headlineLarge)
        Text(
            when (mode) {
                "signup" -> "Crie sua conta para começar."
                "recover" -> "Informe seu e-mail para recuperar o acesso."
                else -> "Entre para acompanhar suas finanças."
            },
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        if (mode == "signup") {
            OutlinedTextField(fullName, { fullName = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(email, onEmailChange, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
        if (mode != "recover") {
            OutlinedTextField(password, onPasswordChange, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth())
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Button(
            onClick = {
                when (mode) {
                    "signup" -> onSignup(fullName)
                    "recover" -> onRecover()
                    else -> onLogin()
                }
            },
            enabled = !loading && email.isNotBlank() && (mode == "recover" || password.isNotBlank()) &&
                (mode != "signup" || fullName.isNotBlank()),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            if (loading) CircularProgressIndicator()
            else Text(when (mode) { "signup" -> "Criar conta"; "recover" -> "Enviar instruções"; else -> "Entrar" })
        }
        TextButton(onClick = {
            mode = when (mode) { "login" -> "signup"; "signup" -> "login"; else -> "login" }
        }) {
            Text(if (mode == "signup") "Já tenho uma conta" else "Criar conta")
        }
        TextButton(onClick = { mode = "recover" }) { Text("Esqueci minha senha") }
    }
}

@Composable
private fun DashboardCards(data: Dashboard) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard("Receitas", data.income)
        SummaryCard("Despesas", data.expenses)
        SummaryCard("Investimentos", data.investments)
        SummaryCard("Saldo", data.balance)
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title)
            Text("R$ ${String.format(Locale("pt", "BR"), "%.2f", amount)}")
        }
    }
}

@Composable
private fun TransactionList(
    items: List<Transaction>,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { transaction ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${transaction.category} · ${transaction.date}", style = MaterialTheme.typography.titleSmall)
                    Text(transaction.description ?: transaction.type)
                    Text("R$ ${String.format(Locale("pt", "BR"), "%.2f", transaction.amount)}")
                    Row {
                        TextButton(onClick = { onEdit(transaction) }) { Text("Editar") }
                        TextButton(onClick = { onDelete(transaction) }) { Text("Excluir") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionFormDialog(
    transaction: Transaction?,
    onDismiss: () -> Unit,
    onSave: (TransactionWrite) -> Unit
) {
    var amount by remember(transaction?.id) { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var type by remember(transaction?.id) { mutableStateOf(transaction?.type ?: "expense") }
    var category by remember(transaction?.id) { mutableStateOf(transaction?.category ?: "") }
    var description by remember(transaction?.id) { mutableStateOf(transaction?.description.orEmpty()) }
    var date by remember(transaction?.id) { mutableStateOf(transaction?.date ?: java.time.LocalDate.now().toString()) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Nova transação" else "Editar transação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Valor") })
                OutlinedTextField(type, { type = it }, label = { Text("Tipo: income / expense / investment") })
                OutlinedTextField(category, { category = it }, label = { Text("Categoria") })
                OutlinedTextField(description, { description = it }, label = { Text("Descrição (opcional)") })
                OutlinedTextField(date, { date = it }, label = { Text("Data (AAAA-MM-DD)") })
                validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
                if (parsedAmount == null || parsedAmount <= 0.0 ||
                    type !in setOf("income", "expense", "investment") ||
                    category.isBlank() || !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                ) {
                    validationMessage = "Confira valor, tipo, categoria e data."
                } else {
                    onSave(TransactionWrite(parsedAmount, type, category.trim(), description.trim().ifBlank { null }, date))
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
