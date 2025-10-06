package com.lewisdeveloping.gedcomviewer.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.PersonCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    individualId: String,
    data: GedcomData,
    onNavigateBack: () -> Unit,
    onIndividualSelected: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = data.individuals[individualId]
    var showDetails by remember(focus?.id) { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
                selected = null,
                onNavigateHome = onNavigateHome,
                onNavigateIndex = onNavigateIndex,
                onOpenFile = onOpenFile
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
                Text(text = "Individual not found", style = MaterialTheme.typography.bodyLarge)
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
                val isCompact = this.maxWidth < 600.dp
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
                        isCompact = isCompact
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
private fun IndividualDetailsDialog(
    individual: Individual,
    onDismissRequest: () -> Unit
) {
    val genderLabel = when (individual.gender) {
        Individual.Gender.MALE -> "Male"
        Individual.Gender.FEMALE -> "Female"
        Individual.Gender.UNKNOWN -> "Unknown"
    }

    val detailItems = buildList {
        add("Full name" to individual.displayName)
        individual.givenName?.takeIf { it.isNotBlank() }?.let { add("Given name" to it) }
        individual.surname?.takeIf { it.isNotBlank() }?.let { add("Surname" to it) }
        add("Gender" to genderLabel)
        individual.birth?.date?.takeIf { it.isNotBlank() }?.let { add("Birth date" to it) }
        individual.birth?.place?.takeIf { it.isNotBlank() }?.let { add("Birth place" to it) }
        individual.death?.date?.takeIf { it.isNotBlank() }?.let { add("Death date" to it) }
        individual.death?.place?.takeIf { it.isNotBlank() }?.let { add("Death place" to it) }
        individual.primaryObjectId?.takeIf { it.isNotBlank() }?.let { add("Primary object" to it) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null
            )
        },
        title = {
            Text(text = "Individual details")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (detailItems.isEmpty() && individual.notes.isEmpty()) {
                    Text(
                        text = "No additional information available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    detailItems.forEach { (label, value) ->
                        DetailRow(label = label, value = value)
                    }

                    if (individual.notes.isNotEmpty()) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            individual.notes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showDetails && focus != null) {
        IndividualDetailsDialog(
            individual = focus,
            onDismissRequest = { showDetails = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndividualDetailsDialog(
    individual: Individual,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val maxDialogHeight = configuration.screenHeightDp.dp * 0.9f
    val scrollState = rememberScrollState()

    val genderLabel = when (individual.gender) {
        Individual.Gender.MALE -> "Male"
        Individual.Gender.FEMALE -> "Female"
        Individual.Gender.UNKNOWN -> "Unknown"
    }

    val detailItems = buildList {
        add("Full name" to individual.displayName)
        individual.givenName?.takeIf { it.isNotBlank() }?.let { add("Given name" to it) }
        individual.surname?.takeIf { it.isNotBlank() }?.let { add("Surname" to it) }
        add("Gender" to genderLabel)
        individual.birth?.date?.takeIf { it.isNotBlank() }?.let { add("Birth date" to it) }
        individual.birth?.place?.takeIf { it.isNotBlank() }?.let { add("Birth place" to it) }
        individual.death?.date?.takeIf { it.isNotBlank() }?.let { add("Death date" to it) }
        individual.death?.place?.takeIf { it.isNotBlank() }?.let { add("Death place" to it) }
        individual.primaryObjectId?.takeIf { it.isNotBlank() }?.let { add("Primary object" to it) }
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isPortrait) 16.dp else 0.dp)
        ) {
            Surface(
                modifier = if (isPortrait) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(min = 360.dp, max = 560.dp)
                },
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Individual details",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (detailItems.isEmpty() && individual.notes.isEmpty()) {
                            Text(
                                text = "No additional information available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            detailItems.forEach { (label, value) ->
                                DetailRow(label = label, value = value)
                            }

                            if (individual.notes.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Notes",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    individual.notes.forEach { note ->
                                        Text(
                                            text = "• $note",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = "Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
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
        ResponsivePersonCardRow(
            isCompact = isCompact,
            first = { modifier ->
                PersonCard(
                    individual = focusParents?.husbandId?.let { data.individuals[it] },
                    label = "Father",
                    modifier = modifier,
                    onClick = onIndividualSelected
                )
            },
            second = { modifier ->
                PersonCard(
                    individual = focusParents?.wifeId?.let { data.individuals[it] },
                    label = "Mother",
                    modifier = modifier,
                    onClick = onIndividualSelected
                )
            }
        )
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Married: $description",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    isCompact: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val header = if (children.isEmpty()) "Children" else "Children (${children.size})"
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (children.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "No recorded children",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val columns = if (isCompact) 2 else if (children.size >= 3) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                children.chunked(columns).forEach { rowChildren ->
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
                        repeat(columns - rowChildren.size) {
                            Spacer(modifier = Modifier.weight(1f))
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
