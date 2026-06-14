package joyin.takgi.paysage.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

data class M3eFabMenuAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun M3eScaffoldBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        content()
    }
}

@Composable
fun M3eTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(start = if (navigationIcon == null) 16.dp else 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (navigationIcon == null) 0.dp else 8.dp)
            )
            actions()
        }
    }
}

@Composable
fun M3eSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp)
        )
        content()
    }
}

@Composable
fun M3eSettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun M3eSettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun M3ePanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    prominent: Boolean = false,
    elevated: Boolean = true,
    content: @Composable () -> Unit
) {
    val container = if (prominent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (prominent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    ElevatedCard(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = container,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (elevated) 2.dp else 0.dp,
            pressedElevation = if (elevated) 4.dp else 0.dp
        )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun M3eStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    icon: ImageVector? = null
) {
    val container = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.heightIn(min = 32.dp),
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun M3eActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = false,
    icon: ImageVector? = null
) {
    val content: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }

    if (prominent) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            content = { content() }
        )
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            content = { content() }
        )
    }
}

@Composable
fun M3eFabAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 56.dp)
            .widthIn(min = 220.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun M3eMainFab(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsedIcon: ImageVector,
    expandedIcon: ImageVector,
    contentDescription: String
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(64.dp),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = if (expanded) expandedIcon else collapsedIcon,
            contentDescription = contentDescription,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun M3eFabMenuHost(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: List<M3eFabMenuAction>,
    collapsedIcon: ImageVector,
    expandedIcon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    endPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    DisposableEffect(Unit) {
        onDispose { onExpandedChange(false) }
    }

    BackHandler(enabled = expanded) {
        onExpandedChange(false)
    }

    val fabCornerRadius by animateDpAsState(
        targetValue = if (expanded) 28.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "m3e_fab_menu_corner"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = fadeOut(animationSpec = tween(durationMillis = 90)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onExpandedChange(false)
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = endPadding, bottom = bottomPadding)
                .zIndex(2f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.forEachIndexed { index, action ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 160, delayMillis = index * 28)
                    ) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(
                            durationMillis = 220,
                            delayMillis = index * 28,
                            easing = LinearEasing
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 90)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(durationMillis = 120)
                        )
                ) {
                    Surface(
                        onClick = {
                            onExpandedChange(false)
                            action.onClick()
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        tonalElevation = 4.dp,
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = action.text,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(fabCornerRadius),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                onClick = { onExpandedChange(!expanded) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (expanded) expandedIcon else collapsedIcon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun M3eSection(
    modifier: Modifier = Modifier,
    title: String,
    supportingText: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = contentColor)
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}
