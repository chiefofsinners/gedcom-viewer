package com.lewisdeveloping.gedcomviewer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class FileActionBarSelection { HOME, INDEX }

@Composable
fun FileActionBar(
    selected: FileActionBarSelection?,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onOpenFile: () -> Unit,
    indexEnabled: Boolean = true
) {
    val homeClick = if (selected == FileActionBarSelection.HOME) ({}) else onNavigateHome
    val indexClick = if (!indexEnabled || selected == FileActionBarSelection.INDEX) ({}) else onNavigateIndex

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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
            selected = false,
            onClick = onOpenFile,
            icon = { Icon(imageVector = Icons.Filled.FolderOpen, contentDescription = "Open GEDCOM") },
            label = { Text(text = "Open") },
            colors = navItemColors
        )
    }
}
