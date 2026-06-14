package joyin.takgi.paysage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private val M3eDropdownMenuShape = RoundedCornerShape(20.dp)
private val M3eDropdownMenuOffset = DpOffset(x = 0.dp, y = 8.dp)

@Composable
fun M3eDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(20.dp),
            small = RoundedCornerShape(20.dp)
        )
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            offset = M3eDropdownMenuOffset,
            shape = M3eDropdownMenuShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
            modifier = modifier
                .widthIn(min = 220.dp, max = 280.dp)
                .shadow(10.dp, M3eDropdownMenuShape)
                .clip(M3eDropdownMenuShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                    shape = M3eDropdownMenuShape
                )
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
    }
}
