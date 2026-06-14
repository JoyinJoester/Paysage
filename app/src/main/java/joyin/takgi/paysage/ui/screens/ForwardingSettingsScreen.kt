package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eSettingsNavigationItem
import joyin.takgi.paysage.ui.components.M3eSettingsSection
import joyin.takgi.paysage.ui.components.M3eTopBar

@Composable
fun ForwardingSettingsScreen(
    onBackClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onMailInboxClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.section_forwarding_settings),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                M3eSettingsSection(title = stringResource(R.string.section_forwarding_settings)) {
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.action_manage_forwarding_accounts),
                        subtitle = stringResource(R.string.summary_manage_forwarding_accounts),
                        onClick = onAccountsClick
                    )
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.action_mail_inbox_command_center),
                        subtitle = stringResource(R.string.summary_mail_inbox_command_center),
                        onClick = onMailInboxClick
                    )
                }
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.summary_forwarding_settings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
