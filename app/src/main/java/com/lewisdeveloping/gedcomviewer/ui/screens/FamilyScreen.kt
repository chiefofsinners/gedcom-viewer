package com.lewisdeveloping.gedcomviewer.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBarSelection
import com.lewisdeveloping.gedcomviewer.ui.components.IndividualDetailsDialog
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanel
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanelStyle
import com.lewisdeveloping.gedcomviewer.ui.components.PersonCard
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    individualId: String,
    data: GedcomData,
    onNavigateBack: () -> Unit,
    onIndividualSelected: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onNavigateFamily: () -> Unit,
    familyEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val focus = data.individuals[individualId]
    var showDetails by remember(focus?.id) { mutableStateOf(false) }
    val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    val swipeModifier = Modifier.pointerInput(onNavigateBack, swipeThreshold) {
        var totalHorizontalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = {
                totalHorizontalDrag = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                totalHorizontalDrag += dragAmount
                if (change.positionChange() != Offset.Zero) change.consume()
            },
            onDragEnd = {
                if (totalHorizontalDrag >= swipeThreshold) {
                    onNavigateBack()
                }
                totalHorizontalDrag = 0f
            },
            onDragCancel = { totalHorizontalDrag = 0f }
        )
    }

    Scaffold(
        modifier = modifier.then(swipeModifier),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(text = focus?.displayName ?: "Family") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (focus != null) {
                        IconButton(onClick = { showDetails = true }) {
                            Icon(imageVector = Icons.Outlined.Info, contentDescription = "Individual details")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.secondaryBackground,
                    scrolledContainerColor = colors.secondaryBackground,
                    titleContentColor = colors.supportingText,
                    navigationIconContentColor = colors.supportingText,
                    actionIconContentColor = colors.supportingText
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
    ) { contentPadding ->
        if (focus == null) {
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InfoPanel(
                    text = "Individual not found",
                    style = InfoPanelStyle.Error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val partnerFamily = findFamily(focus.familiesAsSpouse, data)
            val partner = partnerFamily?.partnerFor(focus, data)
            val focusParents = findFamily(focus.familiesAsChild, data)
            val children = partnerFamily?.childrenIds?.mapNotNull { data.individuals[it] } ?: emptyList()

            BoxWithConstraints(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            ) {
                val configuration = LocalConfiguration.current
                val constraintsMaxWidth = maxWidth
                val constraintsMaxHeight = maxHeight
                val isCompact = constraintsMaxWidth < 600.dp
                val isTablet = configuration.smallestScreenWidthDp >= 600
                val orientation = configuration.orientation
                val stackChildrenVertically = !isTablet &&
                    (isCompact || orientation == Configuration.ORIENTATION_PORTRAIT ||
                        constraintsMaxHeight >= constraintsMaxWidth)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ParentsSection(
                        focusParents = focusParents,
                        data = data,
                        onIndividualSelected = onIndividualSelected,
                        isCompact = isCompact
                    )

                    FamilyCoreSection(
                        focus = focus,
                        partner = partner,
                        marriage = partnerFamily?.marriage,
                        onIndividualSelected = onIndividualSelected,
                        isCompact = isCompact
                    )

                    ChildrenSection(
                        children = children,
                        onIndividualSelected = onIndividualSelected,
                        stackVertically = stackChildrenVertically
                    )
                }
            }
        }
    }

    if (showDetails && focus != null) {
        IndividualDetailsDialog(
            individual = focus,
            onDismissRequest = { showDetails = false }
        )
    }
}
@Composable
private fun ParentsSection(
    focusParents: Family?,
    data: GedcomData,
    onIndividualSelected: (String) -> Unit,
    isCompact: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Parents",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (focusParents == null) {
            InfoPanel(
                text = "No recorded parents.",
                style = InfoPanelStyle.Info,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ResponsivePersonCardRow(
                isCompact = isCompact,
                first = { modifier ->
                    PersonCard(
                        individual = focusParents.husbandId?.let { data.individuals[it] },
                        label = "Father",
                        modifier = modifier,
                        onClick = onIndividualSelected
                    )
                },
                second = { modifier ->
                    PersonCard(
                        individual = focusParents.wifeId?.let { data.individuals[it] },
                        label = "Mother",
                        modifier = modifier,
                        onClick = onIndividualSelected
                    )
                }
            )
        }
    }
}

@Composable
private fun FamilyCoreSection(
    focus: Individual,
    partner: Individual?,
    marriage: LifeEvent?,
    onIndividualSelected: (String) -> Unit,
    isCompact: Boolean
) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Family",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        ResponsivePersonCardRow(
            isCompact = isCompact,
            first = { modifier ->
                PersonCard(
                    individual = focus,
                    label = "Individual",
                    modifier = modifier,
                    onClick = onIndividualSelected
                )
            },
            second = { modifier ->
                PersonCard(
                    individual = partner,
                    label = "Partner",
                    modifier = modifier,
                    onClick = onIndividualSelected
                )
            }
        )
        marriage?.description()?.let { description ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface,
                    contentColor = colors.supportingText
                )
            ) {
                Text(
                    text = "Married: $description",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.supportingText
                )
            }
        }
    }
}

@Composable
private fun ResponsivePersonCardRow(
    isCompact: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    if (isCompact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChildrenSection(
    children: List<Individual>,
    onIndividualSelected: (String) -> Unit,
    stackVertically: Boolean
) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val header = if (children.isEmpty()) "Children" else "Children (${children.size})"
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (children.isEmpty()) {
            InfoPanel(
                text = "No recorded children",
                style = InfoPanelStyle.Info,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            if (stackVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    children.forEach { child ->
                        PersonCard(
                            individual = child,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onIndividualSelected
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    children.chunked(2).forEach { rowChildren ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowChildren.forEach { child ->
                                PersonCard(
                                    individual = child,
                                    modifier = Modifier.weight(1f),
                                    onClick = onIndividualSelected
                                )
                            }
                            repeat(2 - rowChildren.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findFamily(ids: List<String>, data: GedcomData): Family? =
    ids.asSequence().mapNotNull { data.families[it] }.firstOrNull()

private fun Family.partnerFor(individual: Individual, data: GedcomData): Individual? {
    val partnerId = when (individual.id) {
        husbandId -> wifeId
        wifeId -> husbandId
        else -> husbandId ?: wifeId
    }
    return partnerId?.let { data.individuals[it] }
}
