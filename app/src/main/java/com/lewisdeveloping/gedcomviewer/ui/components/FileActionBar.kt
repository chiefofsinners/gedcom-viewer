package com.lewisdeveloping.gedcomviewer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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

    NavigationBar {
        NavigationBarItem(
            selected = selected == FileActionBarSelection.HOME,
            onClick = homeClick,
            icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home") },
            label = { Text(text = "Home") }
        )
        NavigationBarItem(
            selected = selected == FileActionBarSelection.INDEX,
            enabled = indexEnabled,
            onClick = indexClick,
            icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.List, contentDescription = "Index") },
            label = { Text(text = "Index") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenFile,
            icon = { Icon(imageVector = Icons.Filled.FolderOpen, contentDescription = "Open GEDCOM") },
            label = { Text(text = "Open") }
        )
    }
}
