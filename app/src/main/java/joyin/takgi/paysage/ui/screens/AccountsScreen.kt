package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardAccount
import joyin.takgi.paysage.security.ForwardAccountSecretStore
import joyin.takgi.paysage.ui.components.M3eMainFab
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(onBackClick: () -> Unit, onEditAccount: (Int) -> Unit, onAddAccount: () -> Unit) {
    val context = LocalContext.current
    val accountDao = remember { AppDatabase.getDatabase(context).forwardAccountDao() }
    val accounts by accountDao.getAll().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_accounts_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            M3eMainFab(
                expanded = false,
                onClick = onAddAccount,
                collapsedIcon = Icons.Default.Add,
                expandedIcon = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_add_account)
            )
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(stringResource(R.string.message_no_accounts))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(accounts) { account ->
                    AccountItem(account, accountDao, onEditAccount)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountItem(account: ForwardAccount, accountDao: joyin.takgi.paysage.data.ForwardAccountDao, onEdit: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val secretStore = remember { ForwardAccountSecretStore(context) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    M3ePanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(account.id) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                    if (!account.isEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(onClick = {}, label = { Text(context.getString(R.string.status_disabled_chip)) })
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    when (account.type) {
                        AccountType.EMAIL -> context.getString(R.string.prefix_email) + account.toEmail
                        AccountType.TELEGRAM -> context.getString(R.string.prefix_telegram) + account.chatId
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                if (account.phoneWhitelist.isNotEmpty()) {
                    Text(context.getString(R.string.prefix_phone_number) + account.phoneWhitelist, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(androidx.compose.material.icons.Icons.Default.Delete, stringResource(R.string.action_delete))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.title_delete_account)) },
            text = {
                Text(
                    stringResource(R.string.prompt_confirm_delete) +
                        account.name +
                        stringResource(R.string.suffix_question_mark)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            secretStore.clearAccount(account)
                            accountDao.delete(account)
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
