package br.com.kontaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.kontaz.data.ApiFactory
import br.com.kontaz.data.Credentials
import br.com.kontaz.data.Dashboard
import br.com.kontaz.data.Goal
import br.com.kontaz.data.GoalWrite
import br.com.kontaz.data.Profile
import br.com.kontaz.data.ProfileUpdate
import br.com.kontaz.data.RecoveryRequest
import br.com.kontaz.data.SessionStore
import br.com.kontaz.data.SignupRequest
import br.com.kontaz.data.Transaction
import br.com.kontaz.data.TransactionWrite
import br.com.kontaz.ui.FinanceScaffold
import br.com.kontaz.ui.GoalDialog
import br.com.kontaz.ui.GoalsScreen
import br.com.kontaz.ui.HomeDashboard
import br.com.kontaz.ui.KontazColors
import br.com.kontaz.ui.CalculatorsScreen
import br.com.kontaz.ui.ProfileScreen
import br.com.kontaz.ui.TransactionsHistory
import br.com.kontaz.ui.TransactionDialog
import kotlinx.coroutines.launch
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = KontazColors.Green,
                    secondary = KontazColors.Orange,
                    background = KontazColors.Background,
                    surface = KontazColors.Surface,
                    error = KontazColors.Red
                )
            ) {
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
    var displayName by remember { mutableStateOf(sessionStore.displayName() ?: "Bem-vindo(a)") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var dashboard by remember { mutableStateOf<Dashboard?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var goals by remember { mutableStateOf<List<Goal>>(emptyList()) }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var selectedTab by remember { mutableStateOf("dashboard") }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var showTransactionForm by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var goalEditor by remember { mutableStateOf<Goal?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<Goal?>(null) }

    fun loadHome(period: YearMonth = selectedMonth, type: String? = null) {
        scope.launch {
            loading = true
            message = null
            try {
                dashboard = api.dashboard(period.toString())
                transactions = api.transactions(
                    from = period.atDay(1).toString(),
                    to = period.atEndOfMonth().toString(),
                    type = type,
                    limit = 100
                ).items
            } catch (error: Exception) {
                message = error.message ?: "Não foi possível carregar seus dados."
            } finally {
                loading = false
            }
        }
    }

    fun loadGoals() {
        scope.launch {
            loading = true
            message = null
            try {
                goals = api.goals().items
            } catch (error: Exception) {
                message = error.message ?: "Não foi possível carregar suas metas."
            } finally {
                loading = false
            }
        }
    }

    fun loadProfile() {
        scope.launch {
            loading = true
            message = null
            try {
                profile = api.profile()
                displayName = profile?.fullName?.takeIf(String::isNotBlank)
                    ?: profile?.email?.substringBefore("@")
                    ?: "Bem-vindo(a)"
            } catch (error: Exception) {
                message = error.message ?: "Não foi possível carregar seu perfil."
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
                        displayName = sessionStore.displayName()
                            ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
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
                            displayName = sessionStore.displayName()
                                ?: fullName.trim().replaceFirstChar { it.uppercase() }
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
        FinanceScaffold(
            selectedTab = selectedTab,
            onSelectTab = { tab ->
                selectedTab = tab
                when (tab) {
                    "dashboard" -> loadHome()
                    "history" -> loadHome(type = selectedType)
                    "goals" -> loadGoals()
                    "profile" -> loadProfile()
                }
            },
            onAddTransaction = {
                editingTransaction = null
                showTransactionForm = true
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    "dashboard" -> HomeDashboard(
                        displayName = displayName,
                        month = selectedMonth,
                        dashboard = dashboard,
                        transactions = transactions,
                        loading = loading,
                        message = message,
                        onMonthChange = { month ->
                            selectedMonth = month
                            loadHome(period = month)
                        },
                        onRefresh = { loadHome(type = null) },
                        onLogout = {
                            sessionStore.clear()
                            signedIn = false
                            dashboard = null
                            transactions = emptyList()
                        },
                        onOpenProfile = {
                            selectedTab = "profile"
                            loadProfile()
                        },
                        onEdit = { editingTransaction = it; showTransactionForm = true },
                        onDelete = { transactionToDelete = it }
                    )
                    "history" -> TransactionsHistory(
                        month = selectedMonth,
                        transactions = transactions,
                        loading = loading,
                        message = message,
                        selectedType = selectedType,
                        onTypeChange = { type ->
                            selectedType = type
                            loadHome(type = type)
                        },
                        onMonthChange = { month ->
                            selectedMonth = month
                            loadHome(period = month, type = selectedType)
                        },
                        onEdit = { editingTransaction = it; showTransactionForm = true },
                        onDelete = { transactionToDelete = it },
                        onRefresh = { loadHome(type = selectedType) }
                    )
                    "goals" -> GoalsScreen(
                        goals = goals,
                        loading = loading,
                        message = message,
                        onAdd = {
                            goalEditor = null
                            showGoalDialog = true
                        },
                        onEdit = { goal ->
                            goalEditor = goal
                            showGoalDialog = true
                        },
                        onDelete = { goalToDelete = it }
                    )
                    "calculators" -> CalculatorsScreen()
                    "profile" -> ProfileScreen(
                        profile = profile,
                        loading = loading,
                        message = message,
                        onBack = {
                            selectedTab = "dashboard"
                            loadHome()
                        },
                        onEditName = { newName ->
                            scope.launch {
                                loading = true
                                message = null
                                try {
                                    profile = api.updateProfile(ProfileUpdate(newName))
                                    displayName = newName
                                } catch (error: Exception) {
                                    message = error.message ?: "Não foi possível atualizar o perfil."
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        onLogout = {
                            sessionStore.clear()
                            signedIn = false
                            profile = null
                            dashboard = null
                            transactions = emptyList()
                            goals = emptyList()
                        },
                        onRefresh = { loadProfile() }
                    )
                }
            }
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            goal = goalEditor,
            onDismiss = { showGoalDialog = false },
            onSave = { body: GoalWrite ->
                scope.launch {
                    loading = true
                    message = null
                    try {
                        val existingGoal = goalEditor
                        if (existingGoal == null) api.createGoal(body)
                        else api.updateGoal(existingGoal.id, body)
                        showGoalDialog = false
                        loadGoals()
                    } catch (error: Exception) {
                        message = error.message ?: "Não foi possível salvar a meta."
                    } finally {
                        loading = false
                    }
                }
            }
        )
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Excluir meta?") },
            text = { Text("A meta \"${goal.title}\" será removida.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        loading = true
                        try {
                            api.deleteGoal(goal.id)
                            goalToDelete = null
                            loadGoals()
                        } catch (error: Exception) {
                            message = error.message ?: "Não foi possível excluir a meta."
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { goalToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (showTransactionForm) {
        TransactionDialog(
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
                        loadHome(type = if (selectedTab == "history") selectedType else null)
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
                            loadHome(type = if (selectedTab == "history") selectedType else null)
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
        modifier = Modifier.fillMaxSize().background(KontazColors.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("KONTAZ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = KontazColors.Green)
        Text(
            when (mode) {
                "signup" -> "Crie sua conta para começar."
                "recover" -> "Informe seu e-mail para recuperar o acesso."
                else -> "Entre para acompanhar suas finanças."
            },
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            color = KontazColors.Muted
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KontazColors.Surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, KontazColors.Border),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mode == "signup") {
                    OutlinedTextField(fullName, { fullName = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                OutlinedTextField(
                    email,
                    onEmailChange,
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                if (mode != "recover") {
                    OutlinedTextField(
                        password,
                        onPasswordChange,
                        label = { Text("Senha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
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
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    if (loading) CircularProgressIndicator()
                    else Text(when (mode) { "signup" -> "Criar conta"; "recover" -> "Enviar instruções"; else -> "Entrar" })
                }
                TextButton(
                    onClick = {
                        mode = when (mode) { "login" -> "signup"; "signup" -> "login"; else -> "login" }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (mode == "signup") "Já tenho uma conta" else "Criar conta")
                }
                if (mode != "recover") {
                    TextButton(onClick = { mode = "recover" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Esqueci minha senha")
                    }
                } else {
                    TextButton(onClick = { mode = "login" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Voltar para entrar")
                    }
                }
            }
        }
    }
}
