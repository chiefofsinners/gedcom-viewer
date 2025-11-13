package com.lewisdeveloping.gedcomviewer.ui

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lewisdeveloping.gedcomviewer.GedcomViewModel
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBarSelection
import com.lewisdeveloping.gedcomviewer.ui.components.IndividualDetailsDialog
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanel
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanelStyle
import com.lewisdeveloping.gedcomviewer.ui.screens.FamilyScreen
import com.lewisdeveloping.gedcomviewer.ui.screens.IndividualsScreen
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme
import com.lewisdeveloping.gedcomviewer.ui.theme.AppThemeOption
import com.lewisdeveloping.gedcomviewer.ui.theme.GedcomViewerTheme
import com.lewisdeveloping.gedcomviewer.ui.theme.ThemePreferences
import kotlinx.coroutines.launch

@Composable
fun GedcomViewerApp(viewModel: GedcomViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context.applicationContext) }
    val onSelectTheme = remember(themePreferences) {
        { option: AppThemeOption -> themePreferences.select(option) }
    }
    val currentTheme by themePreferences.theme.collectAsState()
    var rootSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var navigationPath by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var suppressSelectionReset by remember { mutableStateOf(false) }
    var currentTab by rememberSaveable { mutableStateOf(FileActionBarSelection.HOME) }
    val showFullScreenLoading = uiState.isLoading && !uiState.needsFileSelection
    val data = uiState.data
    val errorMessage = uiState.error
    val selectedIndividualId = uiState.selectedIndividualId
    val activeIndividualId = navigationPath.lastOrNull() ?: rootSelection

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
        openDocumentLauncher.launch(arrayOf("*/*"))
    }

    LaunchedEffect(uiState.needsFileSelection, activeIndividualId) {
        val familyAvailable = activeIndividualId != null
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

    LaunchedEffect(showFullScreenLoading) {
        if (showFullScreenLoading) {
            currentTab = FileActionBarSelection.INDEX
        }
    }

    LaunchedEffect(selectedIndividualId) {
        if (suppressSelectionReset) {
            suppressSelectionReset = false
        } else {
            rootSelection = selectedIndividualId
            navigationPath = emptyList()
        }
    }

    LaunchedEffect(data) {
        if (data == null) {
            navigationPath = emptyList()
            rootSelection = null
            viewModel.selectIndividual(null)
        } else {
            val rootValid = rootSelection?.let { data.individuals.containsKey(it) } ?: true
            if (!rootValid) {
                rootSelection = null
                navigationPath = emptyList()
                viewModel.selectIndividual(null)
            } else {
                val filtered = navigationPath.filter { id -> data.individuals.containsKey(id) }
                if (filtered.size != navigationPath.size) {
                    navigationPath = filtered
                }
            }
        }
    }

    val popFamily: (exitAtRoot: Boolean) -> Unit = { exitAtRoot ->
        if (navigationPath.isNotEmpty()) {
            navigationPath = navigationPath.dropLast(1)
            val target: String? = navigationPath.lastOrNull() ?: rootSelection
            suppressSelectionReset = true
            viewModel.selectIndividual(target)
            if (navigationPath.isEmpty() && exitAtRoot) {
                currentTab = FileActionBarSelection.INDEX
            }
        } else if (exitAtRoot) {
            currentTab = FileActionBarSelection.INDEX
        }
    }

    BackHandler(enabled = !showFullScreenLoading && currentTab == FileActionBarSelection.FAMILY && navigationPath.isNotEmpty()) {
        popFamily(true)
    }

    val pushFamily: (String) -> Unit = pushFamily@{ individualId ->
        if (rootSelection == null) {
            rootSelection = individualId
            navigationPath = emptyList()
        } else {
            if (navigationPath.lastOrNull() == individualId) {
                currentTab = FileActionBarSelection.FAMILY
                return@pushFamily
            }
            val existingIndex = navigationPath.indexOf(individualId)
            navigationPath = if (existingIndex >= 0) {
                navigationPath.take(existingIndex + 1)
            } else {
                navigationPath + individualId
            }
        }
        suppressSelectionReset = true
        viewModel.selectIndividual(individualId)
        currentTab = FileActionBarSelection.FAMILY
    }

    val handleIndexSelection: (String) -> Unit = { individualId ->
        navigationPath = emptyList()
        rootSelection = individualId
        suppressSelectionReset = false
        viewModel.selectIndividual(individualId)
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
        if (rootSelection == null) return@navigateFamily
        val restored = viewModel.openSavedIndex()
        if (restored) {
            currentTab = FileActionBarSelection.FAMILY
        }
    }

    val familyHistory = remember(rootSelection, navigationPath) {
        buildList {
            rootSelection?.let { add(it) }
            addAll(navigationPath)
        }
    }
    val familyEnabled = activeIndividualId != null
    val hasData = data != null

    GedcomViewerTheme(theme = currentTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                showFullScreenLoading -> LoadingScreen()
                uiState.needsFileSelection || currentTab == FileActionBarSelection.HOME -> {
                    HomeScreen(
                        errorMessage = errorMessage,
                        hasData = hasData,
                        onBrowseFiles = openFilePicker,
                        onLoadSample = viewModel::loadSample,
                        onNavigateIndex = navigateIndex,
                        onNavigateFamily = navigateFamily,
                        familyEnabled = familyEnabled,
                        currentTheme = currentTheme,
                        onThemeSelected = onSelectTheme
                    )
                }
                currentTab == FileActionBarSelection.FAMILY && activeIndividualId != null && data != null -> {
                    if (familyHistory.isEmpty()) {
                        InfoPanel(
                            text = "Select an individual from the index to view family connections.",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            style = InfoPanelStyle.Info
                        )
                    } else {
                        FamilyPagerScreen(
                            historyIds = familyHistory,
                            activeIndividualId = activeIndividualId,
                            data = data,
                            onPopFamily = popFamily,
                            onIndividualSelected = pushFamily,
                            onNavigateHome = navigateHome,
                            onNavigateIndex = navigateIndex,
                            onNavigateFamily = navigateFamily,
                            familyEnabled = familyEnabled
                        )
                    }
                }
                currentTab == FileActionBarSelection.FAMILY && activeIndividualId == null -> {
                    InfoPanel(
                        text = "Select an individual from the index to view family connections.",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        style = InfoPanelStyle.Info
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
                            lastSuccessfulLoadId = uiState.lastSuccessfulLoadId,
                            onNavigateHome = navigateHome,
                            onNavigateIndex = navigateIndex,
                            onNavigateFamily = navigateFamily,
                            familyEnabled = familyEnabled,
                            onIndividualSelected = handleIndexSelection
                        )
                    }
                }
            }

            if (errorMessage != null && data == null && !uiState.isLoading) {
                ErrorState(message = errorMessage, onRetry = viewModel::refresh)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FamilyPagerScreen(
    historyIds: List<String>,
    activeIndividualId: String,
    data: GedcomData,
    onPopFamily: (exitAtRoot: Boolean) -> Unit,
    onIndividualSelected: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onNavigateFamily: () -> Unit,
    familyEnabled: Boolean
) {
    val pagerState = rememberPagerState(
        initialPage = historyIds.lastIndex.coerceAtLeast(0),
        pageCount = { historyIds.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val historyState by rememberUpdatedState(historyIds)
    var ignorePagerSettle by remember { mutableStateOf(false) }
    var detailsIndividualId by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = AppTheme.colors

    LaunchedEffect(historyIds, activeIndividualId) {
        if (historyIds.isEmpty()) return@LaunchedEffect
        val targetIndex = historyIds.indexOf(activeIndividualId).takeIf { it >= 0 } ?: historyIds.lastIndex
        if (pagerState.currentPage != targetIndex) {
            ignorePagerSettle = true
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    LaunchedEffect(historyIds.size, pagerState) {
        if (historyState.isEmpty()) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (ignorePagerSettle) {
                ignorePagerSettle = false
                return@collect
            }
            val lastIndex = (historyState.size - 1).coerceAtLeast(0)
            if (page < lastIndex) {
                repeat(lastIndex - page) {
                    onPopFamily(false)
                }
            }
        }
    }

    val lastIndex = (historyState.size - 1).coerceAtLeast(0)
    val currentPage = pagerState.currentPage.coerceIn(0, lastIndex)
    val currentIndividualId = historyState.getOrNull(currentPage)
    val currentIndividual = currentIndividualId?.let { data.individuals[it] }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentIndividual?.displayName ?: "Family",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (historyState.size > 1 && currentPage > 0) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val previousPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                pagerState.animateScrollToPage(previousPage)
                            }
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentIndividual != null) {
                        IconButton(onClick = { detailsIndividualId = currentIndividual.id }) {
                            Icon(imageVector = Icons.Outlined.Info, contentDescription = "Individual details")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surface,
                    titleContentColor = colors.infoForeground,
                    navigationIconContentColor = colors.infoForeground,
                    actionIconContentColor = colors.infoForeground
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        bottomBar = {
            FileActionBar(
                selected = FileActionBarSelection.FAMILY,
                onNavigateHome = onNavigateHome,
                onNavigateIndex = onNavigateIndex,
                onNavigateFamily = onNavigateFamily,
                familyEnabled = familyEnabled
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            userScrollEnabled = historyIds.size > 1
        ) { page ->
            val individualId = historyState.getOrNull(page) ?: return@HorizontalPager
            FamilyScreen(
                individualId = individualId,
                data = data,
                onIndividualSelected = onIndividualSelected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val dialogIndividual = detailsIndividualId?.let { data.individuals[it] }
    if (dialogIndividual != null) {
        IndividualDetailsDialog(
            individual = dialogIndividual,
            onDismissRequest = { detailsIndividualId = null }
        )
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
    familyEnabled: Boolean,
    currentTheme: AppThemeOption,
    onThemeSelected: (AppThemeOption) -> Unit,
    availableThemes: List<AppThemeOption> = AppThemeOption.values().toList()
) {
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    val colors = AppTheme.colors
    val configuration = LocalConfiguration.current
    val maxContentWidth = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        260.dp
    } else {
        260.dp
    }

    Scaffold(
        containerColor = colors.background,
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
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select a GEDCOM file",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Choose a local GEDCOM file to browse its family tree.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                errorMessage?.let {
                    InfoPanel(
                        text = it,
                        style = InfoPanelStyle.Error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val buttonModifier = Modifier.widthIn(max = maxContentWidth)
                val buttonTextStyle = MaterialTheme.typography.titleMedium

                Button(
                    onClick = onBrowseFiles,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Browse files",
                        style = buttonTextStyle,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                OutlinedButton(
                    onClick = onLoadSample,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Load sample data",
                        style = buttonTextStyle,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                OutlinedButton(
                    onClick = { showThemeDialog = true },
                    modifier = buttonModifier,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Colour theme",
                        style = buttonTextStyle,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = currentTheme,
            availableThemes = availableThemes,
            onThemeSelected = {
                onThemeSelected(it)
                showThemeDialog = false
            },
            onDismissRequest = { showThemeDialog = false }
        )
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

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ThemePickerDialog(
    currentTheme: AppThemeOption,
    availableThemes: List<AppThemeOption>,
    onThemeSelected: (AppThemeOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = AppTheme.colors
    val materialColors = MaterialTheme.colorScheme
    val optionShape = RoundedCornerShape(12.dp)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = materialColors.surface,
        titleContentColor = materialColors.onSurface,
        textContentColor = materialColors.onSurface,
        title = {
            Text(text = "Select Theme")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                availableThemes.forEach { option ->
                    val selected = option == currentTheme
                    val rowBackground = if (selected) {
                        materialColors.secondaryContainer
                    } else {
                        materialColors.surfaceVariant.copy(alpha = 0.6f)
                    }
                    val borderColor = if (selected) {
                        materialColors.primary
                    } else {
                        colors.border.copy(alpha = 0.5f)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(optionShape)
                            .background(rowBackground)
                            .border(width = 1.dp, color = borderColor, shape = optionShape)
                            .clickable { onThemeSelected(option) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onThemeSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = materialColors.primary,
                                unselectedColor = materialColors.onSurfaceVariant
                            )
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = themeDisplayName(option),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = themeDescription(option),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Close")
            }
        }
    )
}

private fun themeDisplayName(option: AppThemeOption): String = when (option) {
    AppThemeOption.SILVER -> "Silver"
    AppThemeOption.EARTH -> "Earth"
}

private fun themeDescription(option: AppThemeOption): String = when (option) {
    AppThemeOption.SILVER -> "Cool metallic neutral."
    AppThemeOption.EARTH -> "Warm organic palette."
}
