package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardAccount
import joyin.takgi.paysage.data.SmtpAuthType
import joyin.takgi.paysage.data.SmtpProvider
import joyin.takgi.paysage.security.ForwardAccountSecretStore
import joyin.takgi.paysage.sender.EmailPayloadEncryption
import joyin.takgi.paysage.sender.EmailSender
import joyin.takgi.paysage.sender.TelegramSender
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.ui.motion.PaysageAnimatedPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class AccountDetailFlow {
    Basics,
    Delivery,
    Security,
    Filtering
}

private fun accountDetailFlowOrder(type: AccountType): List<AccountDetailFlow> =
    if (type == AccountType.EMAIL) {
        listOf(
            AccountDetailFlow.Basics,
            AccountDetailFlow.Delivery,
            AccountDetailFlow.Security,
            AccountDetailFlow.Filtering
        )
    } else {
        listOf(
            AccountDetailFlow.Basics,
            AccountDetailFlow.Delivery,
            AccountDetailFlow.Filtering
        )
    }

private fun accountDetailFlowIndex(flow: AccountDetailFlow, type: AccountType): Int =
    accountDetailFlowOrder(type).indexOf(flow).let { if (it >= 0) it else 0 }

@Composable
fun AccountDetailScreen(accountId: Int?, onBackClick: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val accountDao = remember { AppDatabase.getDatabase(context).forwardAccountDao() }
    val secretStore = remember { ForwardAccountSecretStore(context) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.EMAIL) }
    var isEnabled by remember { mutableStateOf(true) }
    var phoneWhitelist by remember { mutableStateOf("") }

    var smtpProvider by remember { mutableStateOf(SmtpProvider.CUSTOM) }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("587") }
    var smtpUsername by remember { mutableStateOf("") }
    var smtpAuthType by remember { mutableStateOf(SmtpAuthType.PASSWORD) }
    var smtpCredentialRef by remember { mutableStateOf(secretStore.newCredentialRef()) }
    var smtpCredential by remember { mutableStateOf("") }
    var toEmail by remember { mutableStateOf("") }
    var emailEncryptionEnabled by remember { mutableStateOf(false) }
    var emailEncryptionKeyRef by remember { mutableStateOf(secretStore.newEncryptionKeyRef()) }
    var encryptionKey by remember { mutableStateOf("") }
    var originalSmtpCredentialRef by remember { mutableStateOf("") }
    var originalEmailEncryptionKeyRef by remember { mutableStateOf("") }

    var botToken by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }

    var isTesting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var currentFlow by remember { mutableStateOf(AccountDetailFlow.Basics) }

    val flowOrder = accountDetailFlowOrder(type)
    val currentFlowIndex = flowOrder.indexOf(currentFlow).let { if (it >= 0) it else 0 }
    val visibleFlow = flowOrder[currentFlowIndex]
    val currentStepTitle = when (visibleFlow) {
        AccountDetailFlow.Basics -> stringResource(R.string.title_account_basics)
        AccountDetailFlow.Delivery -> if (type == AccountType.EMAIL) {
            stringResource(R.string.title_email_settings)
        } else {
            stringResource(R.string.title_telegram_settings)
        }
        AccountDetailFlow.Security -> stringResource(R.string.title_secure_delivery)
        AccountDetailFlow.Filtering -> stringResource(R.string.title_filtering)
    }
    val stepProgress = stringResource(
        R.string.format_step_progress,
        currentFlowIndex + 1,
        flowOrder.size,
        currentStepTitle
    )

    LaunchedEffect(type) {
        if (currentFlow !in accountDetailFlowOrder(type)) {
            currentFlow = AccountDetailFlow.Delivery
        }
    }

    LaunchedEffect(accountId) {
        if (accountId != null && accountId > 0) {
            accountDao.getAll().first().find { it.id == accountId }?.let { storedAccount ->
                val account = secretStore.migratePlaintextCredential(storedAccount)
                if (account != storedAccount) {
                    accountDao.update(account)
                }
                val credentialRef = secretStore.accountCredentialRef(account)
                val keyRef = secretStore.accountEncryptionKeyRef(account)

                name = account.name
                type = account.type
                isEnabled = account.isEnabled
                phoneWhitelist = account.phoneWhitelist
                smtpProvider = account.smtpProvider
                smtpHost = account.smtpHost
                smtpPort = account.smtpPort.toString()
                smtpUsername = account.smtpUsername
                smtpAuthType = account.smtpAuthType
                smtpCredentialRef = credentialRef
                originalSmtpCredentialRef = credentialRef
                smtpCredential = secretStore.readCredential(credentialRef).ifBlank { account.smtpPassword }
                toEmail = account.toEmail
                emailEncryptionEnabled = account.emailEncryptionEnabled
                emailEncryptionKeyRef = keyRef
                originalEmailEncryptionKeyRef = keyRef
                encryptionKey = secretStore.readEncryptionKey(keyRef)
                botToken = account.botToken
                chatId = account.chatId
            }
        }
    }

    fun applyGmailPreset() {
        smtpProvider = SmtpProvider.GMAIL
        smtpHost = "smtp.gmail.com"
        smtpPort = "587"
        if (smtpUsername.isBlank()) smtpUsername = toEmail
    }

    fun normalizedEncryptionKeyOrNull(): String? {
        if (!emailEncryptionEnabled) return ""
        if (encryptionKey.isBlank()) {
            encryptionKey = EmailPayloadEncryption.generateKeyBase64()
        }
        return EmailPayloadEncryption.normalizeKeyBase64(encryptionKey)
    }

    fun saveAccount() {
        scope.launch {
            if (type == AccountType.EMAIL) {
                if (
                    smtpHost.isBlank() ||
                    smtpUsername.isBlank() ||
                    smtpCredential.isBlank() ||
                    toEmail.isBlank()
                ) {
                    statusMessage = context.getString(R.string.message_smtp_config_incomplete)
                    return@launch
                }
            }

            val credentialRef = smtpCredentialRef.ifBlank { secretStore.newCredentialRef() }
            val keyRef = emailEncryptionKeyRef.ifBlank { secretStore.newEncryptionKeyRef() }
            val normalizedKey = normalizedEncryptionKeyOrNull()
            if (normalizedKey == null) {
                statusMessage = context.getString(R.string.message_encryption_key_invalid)
                return@launch
            }

            if (type == AccountType.EMAIL) {
                if (originalSmtpCredentialRef.isNotBlank() && originalSmtpCredentialRef != credentialRef) {
                    secretStore.clearCredential(originalSmtpCredentialRef)
                }
                secretStore.writeCredential(credentialRef, smtpCredential)
                if (emailEncryptionEnabled) {
                    if (originalEmailEncryptionKeyRef.isNotBlank() && originalEmailEncryptionKeyRef != keyRef) {
                        secretStore.clearEncryptionKey(originalEmailEncryptionKeyRef)
                    }
                    secretStore.writeEncryptionKey(keyRef, normalizedKey)
                    encryptionKey = normalizedKey
                } else {
                    secretStore.clearEncryptionKey(keyRef)
                    if (originalEmailEncryptionKeyRef.isNotBlank() && originalEmailEncryptionKeyRef != keyRef) {
                        secretStore.clearEncryptionKey(originalEmailEncryptionKeyRef)
                    }
                }
            } else {
                originalSmtpCredentialRef.takeIf { it.isNotBlank() }?.let(secretStore::clearCredential)
                originalEmailEncryptionKeyRef.takeIf { it.isNotBlank() }?.let(secretStore::clearEncryptionKey)
            }

            val account = ForwardAccount(
                id = accountId ?: 0,
                name = name.ifBlank { context.getString(R.string.title_unnamed_account) },
                type = type,
                isEnabled = isEnabled,
                phoneWhitelist = phoneWhitelist,
                smtpProvider = smtpProvider,
                smtpHost = smtpHost.trim(),
                smtpPort = smtpPort.toIntOrNull() ?: 587,
                smtpUsername = smtpUsername.trim(),
                smtpPassword = "",
                smtpAuthType = smtpAuthType,
                smtpCredentialRef = if (type == AccountType.EMAIL) credentialRef else "",
                toEmail = toEmail.trim(),
                emailEncryptionEnabled = type == AccountType.EMAIL && emailEncryptionEnabled,
                emailEncryptionKeyRef = if (type == AccountType.EMAIL && emailEncryptionEnabled) keyRef else "",
                botToken = botToken,
                chatId = chatId
            )
            if (accountId == null) {
                accountDao.insert(account)
            } else {
                accountDao.update(account)
            }
            statusMessage = context.getString(R.string.message_account_saved)
            onSaved()
        }
    }

    fun testSend() {
        isTesting = true
        scope.launch {
            val result = if (type == AccountType.EMAIL) {
                val normalizedKey = normalizedEncryptionKeyOrNull()
                if (normalizedKey == null) {
                    isTesting = false
                    statusMessage = context.getString(R.string.message_encryption_key_invalid)
                    return@launch
                }
                EmailSender(
                    smtpHost,
                    smtpPort.toIntOrNull() ?: 587,
                    smtpUsername,
                    smtpCredential,
                    toEmail,
                    context,
                    smtpAuthType,
                    normalizedKey.orEmpty()
                ).send(
                    context.getString(R.string.prefix_test),
                    context.getString(R.string.message_test_content),
                    System.currentTimeMillis()
                )
            } else {
                TelegramSender(botToken, chatId, context)
                    .send(
                        context.getString(R.string.prefix_test),
                        context.getString(R.string.message_test_content),
                        System.currentTimeMillis()
                    )
            }
            isTesting = false
            statusMessage = if (result.isSuccess) {
                context.getString(R.string.message_test_send_success)
            } else {
                context.getString(R.string.message_test_send_failed)
            }
        }
    }

    fun goBackInFlow() {
        if (currentFlowIndex > 0) {
            currentFlow = flowOrder[currentFlowIndex - 1]
        } else {
            onBackClick()
        }
    }

    fun goForwardInFlow() {
        if (currentFlowIndex < flowOrder.lastIndex) {
            currentFlow = flowOrder[currentFlowIndex + 1]
        } else {
            saveAccount()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = if (accountId == null) {
                    stringResource(R.string.title_add_account)
                } else {
                    stringResource(R.string.title_edit_account)
                },
                navigationIcon = {
                    IconButton(onClick = ::goBackInFlow) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stepProgress,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            PaysageAnimatedPage(
                targetState = visibleFlow,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
                isForward = { initialState, targetState ->
                    accountDetailFlowIndex(targetState, type) > accountDetailFlowIndex(initialState, type)
                }
            ) { flow ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (flow) {
                        AccountDetailFlow.Basics -> AccountBasicsPanel(
                            name = name,
                            onNameChange = { name = it },
                            type = type,
                            onTypeChange = { type = it },
                            isEnabled = isEnabled,
                            onEnabledChange = { isEnabled = it }
                        )

                        AccountDetailFlow.Delivery -> {
                            if (type == AccountType.EMAIL) {
                                EmailDeliveryPanel(
                                    smtpProvider = smtpProvider,
                                    onCustomProvider = { smtpProvider = SmtpProvider.CUSTOM },
                                    onGmailProvider = ::applyGmailPreset,
                                    smtpHost = smtpHost,
                                    onHostChange = { smtpHost = it },
                                    smtpPort = smtpPort,
                                    onPortChange = { smtpPort = it },
                                    smtpUsername = smtpUsername,
                                    onUsernameChange = { smtpUsername = it },
                                    smtpAuthType = smtpAuthType,
                                    onAuthTypeChange = { smtpAuthType = it },
                                    smtpCredential = smtpCredential,
                                    onCredentialChange = { smtpCredential = it },
                                    toEmail = toEmail,
                                    onToEmailChange = { toEmail = it }
                                )
                            } else {
                                TelegramConfig(
                                    botToken = botToken,
                                    onBotTokenChange = { botToken = it },
                                    chatId = chatId,
                                    onChatIdChange = { chatId = it }
                                )
                            }
                        }

                        AccountDetailFlow.Security -> EmailSecurityPanel(
                            enabled = emailEncryptionEnabled,
                            onEnabledChange = {
                                emailEncryptionEnabled = it
                                if (it && encryptionKey.isBlank()) {
                                    encryptionKey = EmailPayloadEncryption.generateKeyBase64()
                                }
                            },
                            encryptionKey = encryptionKey,
                            onEncryptionKeyChange = { encryptionKey = it },
                            onGenerateKey = {
                                encryptionKey = EmailPayloadEncryption.generateKeyBase64()
                                statusMessage = context.getString(R.string.message_encryption_key_generated)
                            },
                            onCopyKey = {
                                clipboardManager.setText(AnnotatedString(encryptionKey))
                                statusMessage = context.getString(R.string.message_encryption_key_copied)
                            }
                        )

                        AccountDetailFlow.Filtering -> FilteringPanel(
                            phoneWhitelist = phoneWhitelist,
                            onPhoneWhitelistChange = { phoneWhitelist = it }
                        )
                    }
                }
            }

            AccountDetailActionPanel(
                statusMessage = statusMessage,
                canGoBack = currentFlowIndex > 0,
                isLastStep = currentFlowIndex == flowOrder.lastIndex,
                isTesting = isTesting,
                canTest = name.isNotBlank(),
                onPrevious = ::goBackInFlow,
                onNext = ::goForwardInFlow,
                onTestSend = ::testSend
            )
        }
    }
}

@Composable
private fun AccountDetailActionPanel(
    statusMessage: String,
    canGoBack: Boolean,
    isLastStep: Boolean,
    isTesting: Boolean,
    canTest: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTestSend: () -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f),
                    enabled = canGoBack && !isTesting
                ) {
                    Text(stringResource(R.string.action_previous))
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    enabled = !isTesting
                ) {
                    Text(
                        if (isLastStep) {
                            stringResource(R.string.action_save)
                        } else {
                            stringResource(R.string.action_continue)
                        }
                    )
                }
            }
            if (isLastStep) {
                OutlinedButton(
                    onClick = onTestSend,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTesting && canTest
                ) {
                    Text(if (isTesting) stringResource(R.string.status_sending) else stringResource(R.string.button_test_send))
                }
            }
        }
    }
}

@Composable
private fun FilteringPanel(
    phoneWhitelist: String,
    onPhoneWhitelistChange: (String) -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.title_filtering),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = phoneWhitelist,
                onValueChange = onPhoneWhitelistChange,
                label = { Text(stringResource(R.string.label_phone_whitelist)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AccountBasicsPanel(
    name: String,
    onNameChange: (String) -> Unit,
    type: AccountType,
    onTypeChange: (AccountType) -> Unit,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.title_account_basics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.label_account_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == AccountType.EMAIL,
                    onClick = { onTypeChange(AccountType.EMAIL) },
                    label = { Text(stringResource(R.string.type_email)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = type == AccountType.TELEGRAM,
                    onClick = { onTypeChange(AccountType.TELEGRAM) },
                    label = { Text(stringResource(R.string.type_telegram)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_enable), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
            }
        }
    }
}

@Composable
private fun EmailDeliveryPanel(
    smtpProvider: SmtpProvider,
    onCustomProvider: () -> Unit,
    onGmailProvider: () -> Unit,
    smtpHost: String,
    onHostChange: (String) -> Unit,
    smtpPort: String,
    onPortChange: (String) -> Unit,
    smtpUsername: String,
    onUsernameChange: (String) -> Unit,
    smtpAuthType: SmtpAuthType,
    onAuthTypeChange: (SmtpAuthType) -> Unit,
    smtpCredential: String,
    onCredentialChange: (String) -> Unit,
    toEmail: String,
    onToEmailChange: (String) -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.title_email_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            DirectDeliveryNotice()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = smtpProvider == SmtpProvider.GMAIL,
                    onClick = onGmailProvider,
                    label = { Text(stringResource(R.string.smtp_provider_gmail)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = smtpProvider == SmtpProvider.CUSTOM,
                    onClick = onCustomProvider,
                    label = { Text(stringResource(R.string.smtp_provider_custom)) },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = smtpHost,
                onValueChange = onHostChange,
                label = { Text(stringResource(R.string.label_smtp_server)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = smtpPort,
                onValueChange = onPortChange,
                label = { Text(stringResource(R.string.label_port)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = smtpUsername,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.label_email_account)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = smtpAuthType == SmtpAuthType.PASSWORD,
                    onClick = { onAuthTypeChange(SmtpAuthType.PASSWORD) },
                    label = { Text(stringResource(R.string.smtp_auth_password)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = smtpAuthType == SmtpAuthType.XOAUTH2,
                    onClick = { onAuthTypeChange(SmtpAuthType.XOAUTH2) },
                    label = { Text(stringResource(R.string.smtp_auth_xoauth2)) },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = smtpCredential,
                onValueChange = onCredentialChange,
                label = {
                    Text(
                        if (smtpAuthType == SmtpAuthType.XOAUTH2) {
                            stringResource(R.string.label_oauth2_access_token)
                        } else {
                            stringResource(R.string.label_auth_code_or_password)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = toEmail,
                onValueChange = onToEmailChange,
                label = { Text(stringResource(R.string.label_to_email)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
        }
    }
}

@Composable
private fun DirectDeliveryNotice() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.title_direct_smtp_delivery),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.detail_direct_smtp_delivery),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmailSecurityPanel(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    encryptionKey: String,
    onEncryptionKeyChange: (String) -> Unit,
    onGenerateKey: () -> Unit,
    onCopyKey: () -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.title_secure_delivery),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.hint_end_to_end_encryption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                OutlinedTextField(
                    value = encryptionKey,
                    onValueChange = onEncryptionKeyChange,
                    label = { Text(stringResource(R.string.label_encryption_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onGenerateKey,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_generate))
                    }
                    OutlinedButton(
                        onClick = onCopyKey,
                        modifier = Modifier.weight(1f),
                        enabled = encryptionKey.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_copy))
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramConfig(
    botToken: String,
    onBotTokenChange: (String) -> Unit,
    chatId: String,
    onChatIdChange: (String) -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.title_telegram_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = botToken,
                onValueChange = onBotTokenChange,
                label = { Text("Bot Token") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = chatId,
                onValueChange = onChatIdChange,
                label = { Text("Chat ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
