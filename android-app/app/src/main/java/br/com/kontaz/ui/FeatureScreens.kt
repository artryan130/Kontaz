package br.com.kontaz.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import br.com.kontaz.data.Goal
import br.com.kontaz.data.GoalWrite
import br.com.kontaz.data.Profile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow

@Composable
fun GoalsScreen(
    goals: List<Goal>,
    loading: Boolean,
    message: String?,
    onAdd: () -> Unit,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Metas financeiras", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KontazColors.Text)
                Text("Acompanhe seus objetivos", style = MaterialTheme.typography.bodySmall, color = KontazColors.Muted)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = "Criar meta", tint = KontazColors.Green)
            }
        }
        message?.let { Text(it, modifier = Modifier.padding(horizontal = 18.dp), color = KontazColors.Red) }
        if (loading && goals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KontazColors.Green)
            }
        } else if (goals.isEmpty()) {
            EmptyState("Nenhuma meta criada", "Defina um objetivo e acompanhe seu progresso.")
            Button(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(containerColor = KontazColors.Green)
            ) { Text("Criar minha primeira meta") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(goal, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, onEdit: () -> Unit, onDelete: () -> Unit) {
    val progress = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    var showMenu by remember(goal.id) { mutableStateOf(false) }
    FinanceCard(contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = KontazColors.GreenSoft, shape = CircleShape, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = KontazColors.Green)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(goal.title, color = KontazColors.Text, fontWeight = FontWeight.SemiBold)
                    Text(goal.category, color = KontazColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Opções da meta", tint = KontazColors.Muted)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = KontazColors.Green,
                trackColor = KontazColors.GreenSoft
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMoney(goal.currentAmount), color = KontazColors.Green, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("${(progress * 100).toInt()}%", color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall)
                Text(formatMoney(goal.targetAmount), color = KontazColors.Text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            goal.targetDate?.let {
                Text("Prazo: ${it.toDisplayDate()}", color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun GoalDialog(
    goal: Goal?,
    onDismiss: () -> Unit,
    onSave: (GoalWrite) -> Unit
) {
    var title by remember(goal?.id) { mutableStateOf(goal?.title.orEmpty()) }
    var target by remember(goal?.id) { mutableStateOf(goal?.targetAmount?.let(::decimalInput).orEmpty()) }
    var current by remember(goal?.id) { mutableStateOf(goal?.currentAmount?.let(::decimalInput) ?: "0") }
    var category by remember(goal?.id) { mutableStateOf(goal?.category ?: "financial") }
    var targetDate by remember(goal?.id) { mutableStateOf(goal?.targetDate.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val dismissKeyboardActions = rememberDismissKeyboardActions()
    val categoryOptions = listOf(
        "financial" to "Geral",
        "travel" to "Viagem",
        "home" to "Moradia",
        "vehicle" to "Veículo",
        "education" to "Educação",
        "emergency" to "Reserva de emergência",
        "other" to "Outro"
    ).let { options ->
        if (options.any { it.first == category }) options else options + (category to category)
    }
    val openDatePicker = {
        val initial = runCatching { LocalDate.parse(targetDate) }.getOrDefault(LocalDate.now())
        DatePickerDialog(
            context,
            { _, year, month, day ->
                targetDate = LocalDate.of(year, month + 1, day).toString()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable {}.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = KontazColors.Surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    if (goal == null) "Nova meta" else "Editar meta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = KontazColors.Text
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da meta") },
                    modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = dismissKeyboardActions
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = normalizeDecimalInput(it) },
                    label = { Text("Valor pretendido (R$)") },
                    modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = dismissKeyboardActions,
                    visualTransformation = BrazilianNumberVisualTransformation(),
                    supportingText = { Text("Os dois últimos dígitos são os centavos.") }
                )
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = normalizeDecimalInput(it) },
                    label = { Text("Valor já guardado (R$)") },
                    modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = dismissKeyboardActions,
                    visualTransformation = BrazilianNumberVisualTransformation(),
                    supportingText = { Text("Os dois últimos dígitos são os centavos.") }
                )
                Box {
                    OutlinedTextField(
                        value = categoryOptions.firstOrNull { it.first == category }?.second ?: category,
                        onValueChange = {},
                        label = { Text("Categoria") },
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showCategoryMenu = true }) {
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Selecionar categoria")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showCategoryMenu = true }
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        categoryOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    category = value
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetDate.takeIf(String::isNotBlank)?.let(::formatGoalDate).orEmpty(),
                    onValueChange = {},
                    label = { Text("Prazo (opcional)") },
                    placeholder = { Text("Selecione no calendário") },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            openDatePicker()
                        }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Selecionar prazo")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { openDatePicker() }
                )
                if (targetDate.isNotBlank()) {
                    TextButton(onClick = { targetDate = "" }) { Text("Remover prazo") }
                }
                    error?.let { Text(it, color = KontazColors.Red, style = MaterialTheme.typography.bodySmall) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = {
                val targetValue = target.toDecimalOrNull()
                val currentValue = current.toDecimalOrNull()
                val validDate = targetDate.isBlank() || runCatching { LocalDate.parse(targetDate) }.isSuccess
                if (title.isBlank() || targetValue == null || targetValue <= 0.0 ||
                    currentValue == null || currentValue < 0.0 || category.isBlank() || !validDate
                ) {
                    error = "Confira nome, valores, categoria e formato do prazo."
                } else {
                    onSave(
                        GoalWrite(
                            title = title.trim(),
                            targetAmount = targetValue,
                            currentAmount = currentValue,
                            category = category.trim(),
                            iconType = goal?.iconType ?: "piggy",
                            targetDate = targetDate.trim().ifBlank { null }
                        )
                    )
                }
                    }) { Text("Salvar") }
                }
            }
        }
    }
}

@Composable
fun CalculatorsScreen() {
    var selectedCalculator by remember { mutableStateOf("compound") }
    var principal by remember { mutableStateOf("") }
    var monthlyContribution by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    val amount = principal.toDecimalOrNull() ?: 0.0
    val contribution = monthlyContribution.toDecimalOrNull() ?: 0.0
    val interestRate = rate.toDecimalOrNull()?.div(100) ?: 0.0
    val periodCount = months.toIntOrNull() ?: 0
    val compoundValue = calculateCompound(amount, contribution, interestRate, periodCount)

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Calculadoras", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KontazColors.Text)
                Text("Simule seus próximos passos financeiros", color = KontazColors.Muted)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorChoice("Juros compostos", selectedCalculator == "compound", Modifier.weight(1f)) { selectedCalculator = "compound" }
                CalculatorChoice("Parcelas", selectedCalculator == "installments", Modifier.weight(1f)) { selectedCalculator = "installments" }
            }
        }
        item {
            FinanceCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (selectedCalculator == "compound") "Simulador de juros compostos" else "Simulador de parcelas",
                        color = KontazColors.Text,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    CalculatorField(
                        if (selectedCalculator == "compound") "Valor inicial (R$)" else "Valor total (R$)",
                        principal
                    ) { principal = it }
                    if (selectedCalculator == "compound") {
                        CalculatorField("Aporte mensal (R$)", monthlyContribution) { monthlyContribution = it }
                    }
                    CalculatorField("Taxa mensal (%)", rate, currency = false) { rate = it }
                    CalculatorField("Período (meses)", months, numeric = true) { months = it }
                }
            }
        }
        item {
            FinanceCard(contentPadding = 18.dp, borderColor = KontazColors.Green.copy(alpha = 0.4f)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (selectedCalculator == "compound") "Valor estimado ao final" else "Parcela mensal estimada",
                        color = KontazColors.Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    val result = if (selectedCalculator == "compound") compoundValue
                    else calculateInstallment(amount, interestRate, periodCount)
                    Text(
                        formatMoney(result),
                        color = KontazColors.Green,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedCalculator == "compound") {
                        Text("Estimativa matemática; não representa garantia de rendimento.", color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text("Estimativa com parcelas fixas; não inclui impostos ou tarifas adicionais.", color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) KontazColors.GreenSoft else KontazColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) KontazColors.Green else KontazColors.Border),
        modifier = modifier
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = if (selected) KontazColors.Green else KontazColors.Muted, maxLines = 1)
        }
    }
}

@Composable
private fun CalculatorField(
    label: String,
    value: String,
    numeric: Boolean = false,
    currency: Boolean = true,
    onChange: (String) -> Unit
) {
    val dismissKeyboardActions = rememberDismissKeyboardActions()
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(if (numeric) it.filter(Char::isDigit) else normalizeDecimalInput(it)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = dismissKeyboardActions,
        visualTransformation = if (numeric) VisualTransformation.None else BrazilianNumberVisualTransformation(currency),
        supportingText = if (numeric) null else ({ Text("Os dois últimos dígitos são os centavos.") })
    )
}

@Composable
fun ProfileScreen(
    profile: Profile?,
    loading: Boolean,
    message: String?,
    onBack: () -> Unit,
    onEditName: (String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf("") }
    var editName by remember(profile?.fullName) { mutableStateOf(profile?.fullName.orEmpty()) }
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = KontazColors.Muted)
            }
            Text("Perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KontazColors.Text)
        }
        if (loading && profile == null) {
            CircularProgressIndicator(color = KontazColors.Green)
        } else if (profile != null) {
            FinanceCard(contentPadding = 20.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(color = KontazColors.GreenSoft, shape = CircleShape, modifier = Modifier.size(76.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = KontazColors.Green, modifier = Modifier.size(54.dp))
                        }
                    }
                    Text(profile.fullName ?: "Minha conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = KontazColors.Text)
                    Text(profile.email ?: "E-mail não informado", color = KontazColors.Muted)
                    TextButton(onClick = { editName = profile.fullName.orEmpty(); showNameDialog = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Editar nome")
                    }
                }
            }
            FinanceCard(contentPadding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Savings, contentDescription = null, tint = KontazColors.Green)
                    Column {
                        Text("Kontaz", color = KontazColors.Text, fontWeight = FontWeight.SemiBold)
                        Text("Seu controle financeiro pessoal", color = KontazColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        message?.let { Text(it, color = KontazColors.Red) }
        if (profile == null && !loading) {
            TextButton(onClick = onRefresh) { Text("Tentar novamente") }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = KontazColors.OrangeSoft)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = KontazColors.Orange)
            Spacer(Modifier.width(8.dp))
            Text("Sair da conta", color = KontazColors.Orange)
        }
        TextButton(
            onClick = { deleteConfirmation = ""; showDeleteAccountDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {
            Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = KontazColors.Red)
            Spacer(Modifier.width(8.dp))
            Text("Excluir conta e dados", color = KontazColors.Red)
        }
        TextButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.fillMaxWidth()) {
            Text("Política de Privacidade", color = KontazColors.Muted)
        }
    }
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Editar nome") },
            text = {
                val dismissKeyboardActions = rememberDismissKeyboardActions()
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nome") },
                    modifier = Modifier.dismissKeyboardOnEnter(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = dismissKeyboardActions
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) {
                        onEditName(editName.trim())
                        showNameDialog = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancelar") } }
        )
    }
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Excluir conta permanentemente?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sua conta e seus dados associados, incluindo perfil, transações e metas, serão removidos do Kontaz Android e do app web que usa a mesma conta. Esta ação não pode ser desfeita.")
                    val dismissKeyboardActions = rememberDismissKeyboardActions()
                    OutlinedTextField(
                        value = deleteConfirmation,
                        onValueChange = { deleteConfirmation = it },
                        label = { Text("Digite EXCLUIR para confirmar") },
                        modifier = Modifier.dismissKeyboardOnEnter(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = dismissKeyboardActions
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    },
                    enabled = deleteConfirmation == "EXCLUIR" && !loading,
                    colors = ButtonDefaults.textButtonColors(contentColor = KontazColors.Red)
                ) { Text("Excluir definitivamente") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun PasswordResetScreen(
    loading: Boolean,
    message: String?,
    onSubmit: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    val passwordsMatch = password == confirmation
    Column(
        modifier = Modifier.fillMaxSize().background(KontazColors.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Redefinir senha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KontazColors.Text)
        Spacer(Modifier.height(16.dp))
        FinanceCard(contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nova senha") },
                    modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = rememberDismissKeyboardActions(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Confirme a nova senha") },
                    modifier = Modifier.fillMaxWidth().dismissKeyboardOnEnter(),
                    singleLine = true,
                    visualTransformation = if (confirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = rememberDismissKeyboardActions(),
                    trailingIcon = {
                        IconButton(onClick = { confirmationVisible = !confirmationVisible }) {
                            Icon(
                                imageVector = if (confirmationVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (confirmationVisible) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    }
                )
                message?.let { Text(it, color = KontazColors.Red, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = { onSubmit(password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading && password.length >= 6 && passwordsMatch
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Salvar nova senha")
                }
                if (confirmation.isNotEmpty() && !passwordsMatch) {
                    Text("As senhas não coincidem.", color = KontazColors.Red, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun calculateCompound(principal: Double, monthlyContribution: Double, monthlyRate: Double, months: Int): Double {
    if (months !in 1..1200 || principal < 0 || monthlyContribution < 0 || monthlyRate < 0) return 0.0
    if (monthlyRate == 0.0) return principal + monthlyContribution * months
    val growth = (1 + monthlyRate).pow(months)
    val contributions = monthlyContribution * (growth - 1) / monthlyRate
    return (principal * growth + contributions).takeIf(Double::isFinite) ?: 0.0
}

private fun calculateInstallment(principal: Double, monthlyRate: Double, months: Int): Double {
    if (months !in 1..1200 || principal <= 0 || monthlyRate < 0) return 0.0
    if (monthlyRate == 0.0) return principal / months
    val discountFactor = (1 + monthlyRate).pow(-months)
    val payment = principal * monthlyRate / (1 - discountFactor)
    return payment.takeIf(Double::isFinite) ?: 0.0
}

private fun formatMoney(value: Double): String =
    String.format(Locale("pt", "BR"), "R$ %,.2f", value)

private fun String.toDisplayDate(): String = runCatching {
    val date = LocalDate.parse(this)
    String.format(Locale("pt", "BR"), "%02d/%02d/%04d", date.dayOfMonth, date.monthValue, date.year)
}.getOrDefault(this)

private fun formatGoalDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")))
}.getOrDefault(value)
