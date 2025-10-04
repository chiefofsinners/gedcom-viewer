package com.lewisdeveloping.gedcomviewer.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lewisdeveloping.gedcomviewer.GedcomViewModel
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBarSelection
import com.lewisdeveloping.gedcomviewer.ui.screens.FamilyScreen
import com.lewisdeveloping.gedcomviewer.ui.screens.IndividualsScreen
import com.lewisdeveloping.gedcomviewer.ui.theme.GedcomViewerTheme

@Composable
fun GedcomViewerApp(viewModel: GedcomViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    val data = uiState.data
    val errorMessage = uiState.error

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

    GedcomViewerTheme {
        when {
            uiState.needsFileSelection -> {
                FileSelectionScreen(
                    errorMessage = errorMessage,
                    onSelectFile = openFilePicker,
                    onLoadSample = viewModel::loadSample,
                    onNavigateHome = viewModel::showHome,
                    onNavigateIndex = viewModel::openSavedIndex,
                    onOpenFile = openFilePicker
                )
            }
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            data != null -> {
                LaunchedEffect(uiState.currentDocumentUri, uiState.isSampleData) {
                    navController.navigate(Routes.Individuals) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.Individuals
                ) {
                    composable(Routes.Individuals) {
                        IndividualsScreen(
                            individuals = data.individualsSortedByName,
                            currentFileName = uiState.currentFileName,
                            onOpenFile = openFilePicker,
                            onNavigateHome = viewModel::showHome,
                            onNavigateIndex = {
                                navController.navigate(Routes.Individuals) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                }
                            },
                            onIndividualSelected = { id ->
                                navController.navigate("${Routes.Family}/$id")
                            }
                        )
                    }
                    composable(
                        route = "${Routes.Family}/{${Routes.FamilyArg}}",
                        arguments = listOf(navArgument(Routes.FamilyArg) { type = NavType.StringType })
                    ) { entry ->
                        val individualId = entry.arguments?.getString(Routes.FamilyArg) ?: return@composable
                        FamilyScreen(
                            individualId = individualId,
                            data = data,
                            onNavigateBack = { navController.popBackStack() },
                            onIndividualSelected = { targetId ->
                                if (targetId != individualId) {
                                    navController.navigate("${Routes.Family}/$targetId")
                                }
                            },
                            onNavigateHome = viewModel::showHome,
                            onNavigateIndex = {
                                navController.navigate(Routes.Individuals) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                }
                            },
                            onOpenFile = openFilePicker
                        )
                    }
                }
            }
            errorMessage != null -> {
                ErrorState(message = errorMessage, onRetry = viewModel::refresh)
            }
        }
    }
}

@Composable
private fun FileSelectionScreen(
    errorMessage: String?,
    onSelectFile: () -> Unit,
    onLoadSample: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onOpenFile: () -> Unit
) {
    Scaffold(
        bottomBar = {
            FileActionBar(
                selected = FileActionBarSelection.HOME,
                onNavigateHome = onNavigateHome,
                onNavigateIndex = onNavigateIndex,
                onOpenFile = onOpenFile
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
                modifier = Modifier
                    .padding(24.dp),
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
                Button(onClick = onSelectFile) {
                    Text(text = "Browse files")
                }
                OutlinedButton(onClick = onLoadSample) {
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

private object Routes {
    const val Individuals = "individuals"
    const val Family = "family"
    const val FamilyArg = "individualId"
}
