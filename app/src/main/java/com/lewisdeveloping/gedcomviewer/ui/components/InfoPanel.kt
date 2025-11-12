package com.lewisdeveloping.gedcomviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme

enum class InfoPanelStyle { Info, Error }

@Composable
fun InfoPanel(
    text: String,
    modifier: Modifier = Modifier,
    style: InfoPanelStyle = InfoPanelStyle.Info,
    leadingIcon: ImageVector? = null,
    contentDescription: String? = null
) {
    val colors = AppTheme.colors
    val background = when (style) {
        InfoPanelStyle.Info -> colors.infoBackground
        InfoPanelStyle.Error -> colors.alertBackground
    }
    val foreground = when (style) {
        InfoPanelStyle.Info -> colors.infoForeground
        InfoPanelStyle.Error -> colors.alertForeground
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = contentDescription,
                    tint = foreground
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = foreground
            )
        }
    }
}
