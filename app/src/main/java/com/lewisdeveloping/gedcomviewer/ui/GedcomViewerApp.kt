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
    val startDestination = if (uiState.needsFileSelection && uiState.data == null) {
        Routes.Home
    } else {
        Routes.Individuals
    }

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

    val navigateToHome: () -> Unit = {
        if (navController.currentDestination?.route != Routes.Home) {
            navController.navigate(Routes.Home) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val navigateToIndex: () -> Unit = {
        if (navController.currentDestination?.route != Routes.Individuals) {
            val popped = navController.popBackStack(Routes.Individuals, false)
            if (!popped) {
                navController.navigate(Routes.Individuals) {
                    popUpTo(Routes.Home) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(uiState.needsFileSelection, uiState.data) {
        if (uiState.needsFileSelection) {
            navigateToHome()
        } else if (uiState.data != null && navController.currentDestination?.route == Routes.Home) {
            navigateToIndex()
        }
    }

    GedcomViewerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Routes.Home) {
                    HomeScreen(
                        errorMessage = errorMessage,
                        hasData = data != null,
                        onBrowseFiles = openFilePicker,
                        onLoadSample = viewModel::loadSample,
                        onNavigateIndex = {
                            val navigated = viewModel.openSavedIndex()
                            if (navigated) {
                                navigateToIndex()
                            }
                        }
                    )
                }
                composable(Routes.Individuals) {
                    if (data == null) {
                        // Should not normally happen, but guard to avoid crashes.
                        Text(
                            text = "No GEDCOM data loaded",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        IndividualsScreen(
                            individuals = data.individualsSortedByName,
                            currentFileName = uiState.currentFileName,
                            onOpenFile = openFilePicker,
                            onNavigateHome = {
                                viewModel.showHome()
                                navigateToHome()
                            },
                            onNavigateIndex = { /* Already on index, no-op */ },
                            onIndividualSelected = { id ->
                                navController.navigate("${Routes.Family}/$id")
                            }
                        )
                    }
                }
                composable(
                    route = "${Routes.Family}/{${Routes.FamilyArg}}",
                    arguments = listOf(navArgument(Routes.FamilyArg) { type = NavType.StringType })
                ) { entry ->
                    val individualId = entry.arguments?.getString(Routes.FamilyArg) ?: return@composable
                    if (data == null) {
                        Text(
                            text = "Family data unavailable",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        FamilyScreen(
                            individualId = individualId,
                            data = data,
                            onNavigateBack = { navController.popBackStack() },
                            onIndividualSelected = { targetId ->
                                if (targetId != individualId) {
                                    navController.navigate("${Routes.Family}/$targetId")
                                }
                            },
                            onNavigateHome = {
                                viewModel.showHome()
                                navigateToHome()
                            },
                            onNavigateIndex = {
                                val navigated = viewModel.openSavedIndex()
                                if (navigated) {
                                    navigateToIndex()
                                }
                            },
                            onOpenFile = openFilePicker
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
    onNavigateIndex: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FileActionBar(
                selected = FileActionBarSelection.HOME,
                onNavigateHome = {},
                onNavigateIndex = onNavigateIndex,
                onOpenFile = onBrowseFiles,
                indexEnabled = hasData
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

private object Routes {
    const val Home = "home"
    const val Individuals = "individuals"
    const val Family = "family"
    const val FamilyArg = "individualId"
}
