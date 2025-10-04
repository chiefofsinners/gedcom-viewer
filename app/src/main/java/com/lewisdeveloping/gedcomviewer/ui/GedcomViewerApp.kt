package com.lewisdeveloping.gedcomviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lewisdeveloping.gedcomviewer.GedcomViewModel
import com.lewisdeveloping.gedcomviewer.ui.screens.FamilyScreen
import com.lewisdeveloping.gedcomviewer.ui.screens.IndividualsScreen
import com.lewisdeveloping.gedcomviewer.ui.theme.GedcomViewerTheme

@Composable
fun GedcomViewerApp(viewModel: GedcomViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val errorMessage = uiState.error
    val data = uiState.data

    GedcomViewerTheme {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                ErrorState(message = errorMessage, onRetry = viewModel::refresh)
            }
            data != null -> {
                NavHost(
                    navController = navController,
                    startDestination = Routes.Individuals
                ) {
                    composable(Routes.Individuals) {
                        IndividualsScreen(
                            individuals = data.individualsSortedByName,
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
                                    navController.navigate("${Routes.Family}/$targetId") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
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
