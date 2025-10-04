package com.lewisdeveloping.gedcomviewer.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lewisdeveloping.gedcomviewer.R
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBarSelection
import com.lewisdeveloping.gedcomviewer.ui.components.PersonRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualsScreen(
    individuals: List<Individual>,
    currentFileName: String?,
    onOpenFile: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onIndividualSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = currentFileName?.ifBlank { null } ?: stringResource(id = R.string.app_name))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        bottomBar = {
            FileActionBar(
                selected = FileActionBarSelection.INDEX,
                onNavigateHome = onNavigateHome,
                onNavigateIndex = onNavigateIndex,
                onOpenFile = onOpenFile
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            items(individuals, key = { it.id }) { individual ->
                PersonRow(
                    individual = individual,
                    supportingText = individual.birth?.description(),
                    onClick = onIndividualSelected
                )
            }
        }
    }
}
