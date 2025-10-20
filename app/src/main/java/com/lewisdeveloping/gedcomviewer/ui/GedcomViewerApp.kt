package com.lewisdeveloping.gedcomviewer.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lewisdeveloping.gedcomviewer.GedcomViewModel
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBarSelection
import com.lewisdeveloping.gedcomviewer.ui.screens.FamilyScreen
import com.lewisdeveloping.gedcomviewer.ui.screens.IndividualsScreen
import com.lewisdeveloping.gedcomviewer.ui.theme.GedcomViewerTheme

@Composable
fun GedcomViewerApp(viewModel: GedcomViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val data = uiState.data
    val errorMessage = uiState.error

    var familyStack by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var currentTab by rememberSaveable { mutableStateOf(FileActionBarSelection.HOME) }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Permission might already be granted; ignore.
            }
            viewModel.loadFromUri(it)
        }
    }
    val openFilePicker: () -> Unit = {
        openDocumentLauncher.launch(arrayOf("application/octet-stream", "text/plain", "application/x-gedcom"))
    }

    LaunchedEffect(uiState.needsFileSelection, familyStack) {
        val familyAvailable = familyStack.isNotEmpty()
        val desiredTab = when {
            uiState.needsFileSelection -> FileActionBarSelection.HOME
            currentTab == FileActionBarSelection.HOME -> if (familyAvailable) FileActionBarSelection.FAMILY else FileActionBarSelection.INDEX
            currentTab == FileActionBarSelection.FAMILY && !familyAvailable -> FileActionBarSelection.INDEX
            else -> currentTab
        }
        if (desiredTab != currentTab) {
            currentTab = desiredTab
        }
    }

    LaunchedEffect(data) {
        if (data == null) {
            familyStack = emptyList()
        } else {
            val filtered = familyStack.filter { id -> data.individuals.containsKey(id) }
            if (filtered.size != familyStack.size) {
                familyStack = filtered
            }
        }
    }

    val popFamily: () -> Unit = {
        if (familyStack.isNotEmpty()) {
            familyStack = familyStack.dropLast(1)
        }
        if (familyStack.isEmpty()) {
            currentTab = FileActionBarSelection.INDEX
        }
    }

    BackHandler(enabled = currentTab == FileActionBarSelection.FAMILY && familyStack.isNotEmpty()) {
        popFamily()
    }

    val pushFamily: (String) -> Unit = { individualId ->
        if (familyStack.lastOrNull() != individualId) {
            familyStack = familyStack + individualId
        }
        currentTab = FileActionBarSelection.FAMILY
    }

    val navigateHome: () -> Unit = {
        viewModel.showHome()
        currentTab = FileActionBarSelection.HOME
    }

    val navigateIndex: () -> Unit = {
        val restored = viewModel.openSavedIndex()
        if (restored) {
            currentTab = FileActionBarSelection.INDEX
        }
    }

    val navigateFamily: () -> Unit = navigateFamily@{
        if (familyStack.isEmpty()) return@navigateFamily
        val restored = viewModel.openSavedIndex()
        if (restored) {
            currentTab = FileActionBarSelection.FAMILY
        }
    }

    val selectedIndividualId = familyStack.lastOrNull()
    val familyEnabled = selectedIndividualId != null
    val hasData = data != null

    GedcomViewerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.needsFileSelection || currentTab == FileActionBarSelection.HOME -> {
                    HomeScreen(
                        errorMessage = errorMessage,
                        hasData = hasData,
                        onBrowseFiles = openFilePicker,
                        onLoadSample = viewModel::loadSample,
                        onNavigateIndex = navigateIndex,
                        onNavigateFamily = navigateFamily,
                        familyEnabled = familyEnabled
                    )
                }
                currentTab == FileActionBarSelection.FAMILY && selectedIndividualId != null && data != null -> {
                    FamilyScreen(
                        individualId = selectedIndividualId,
                        data = data,
                        onNavigateBack = popFamily,
                        onIndividualSelected = pushFamily,
                        onNavigateHome = navigateHome,
                        onNavigateIndex = navigateIndex,
                        onNavigateFamily = navigateFamily,
                        familyEnabled = familyEnabled
                    )
                }
                else -> {
                    if (data == null) {
                        when {
                            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize())
                            uiState.needsFileSelection -> {
                                Text(
                                    text = "No GEDCOM data loaded",
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            else -> Box(modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        IndividualsScreen(
                            individuals = data.individualsSortedByName,
                            currentFileName = uiState.currentFileName,
                            onNavigateHome = navigateHome,
                            onNavigateIndex = navigateIndex,
                            onNavigateFamily = navigateFamily,
                            familyEnabled = familyEnabled,
                            onIndividualSelected = pushFamily
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (errorMessage != null && data == null && !uiState.isLoading) {
                ErrorState(message = errorMessage, onRetry = viewModel::refresh)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    errorMessage: String?,
    hasData: Boolean,
    onBrowseFiles: () -> Unit,
    onLoadSample: () -> Unit,
    onNavigateIndex: () -> Unit,
    onNavigateFamily: () -> Unit,
    familyEnabled: Boolean
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FileActionBar(
                selected = FileActionBarSelection.HOME,
                onNavigateHome = {},
                onNavigateIndex = onNavigateIndex,
                onNavigateFamily = onNavigateFamily,
                indexEnabled = hasData,
                familyEnabled = familyEnabled
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select a GEDCOM file",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Choose a local GEDCOM file to browse its family tree.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = onBrowseFiles,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Browse files")
                }
                OutlinedButton(
                    onClick = onLoadSample,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Load sample data")
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "Something went wrong", style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}
