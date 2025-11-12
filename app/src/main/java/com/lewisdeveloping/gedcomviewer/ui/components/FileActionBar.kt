package com.lewisdeveloping.gedcomviewer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme

enum class FileActionBarSelection { HOME, INDEX, FAMILY }

@Composable
fun FileActionBar(
    selected: FileActionBarSelection?,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onNavigateFamily: () -> Unit,
    indexEnabled: Boolean = true,
    familyEnabled: Boolean = true
) {
    val homeClick = if (selected == FileActionBarSelection.HOME) ({}) else onNavigateHome
    val indexClick = if (!indexEnabled || selected == FileActionBarSelection.INDEX) ({}) else onNavigateIndex
    val familyClick = if (!familyEnabled || selected == FileActionBarSelection.FAMILY) ({}) else onNavigateFamily

    val colors = AppTheme.colors

    NavigationBar(
        containerColor = colors.tabBackground,
        contentColor = colors.tabForeground,
        tonalElevation = 0.dp
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.tabForeground,
            selectedTextColor = colors.tabForeground,
            indicatorColor = colors.tabBackgroundEmphasis,
            unselectedIconColor = colors.tabInactiveForeground,
            unselectedTextColor = colors.tabInactiveForeground,
            disabledIconColor = colors.tabInactiveForeground.copy(alpha = 0.4f),
            disabledTextColor = colors.tabInactiveForeground.copy(alpha = 0.4f)
        )
        NavigationBarItem(
            selected = selected == FileActionBarSelection.HOME,
            onClick = homeClick,
            icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home") },
            label = { Text(text = "Home") },
            colors = navItemColors
        )
        NavigationBarItem(
            selected = selected == FileActionBarSelection.INDEX,
            enabled = indexEnabled,
            onClick = indexClick,
            icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.List, contentDescription = "Index") },
            label = { Text(text = "Index") },
            colors = navItemColors
        )
        NavigationBarItem(
            selected = selected == FileActionBarSelection.FAMILY,
            enabled = familyEnabled,
            onClick = familyClick,
            icon = { Icon(imageVector = Icons.Filled.Group, contentDescription = "Family connections") },
            label = { Text(text = "Family") },
            colors = navItemColors
        )
    }
}
