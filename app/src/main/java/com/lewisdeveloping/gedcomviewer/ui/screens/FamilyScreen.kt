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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import com.lewisdeveloping.gedcomviewer.model.TimelineEntry
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndividualDetailsDialog(
    individual: Individual,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxDialogHeight = configuration.screenHeightDp.dp * 0.9f
    val scrollState = rememberScrollState()
    val isPhonePortrait = configuration.smallestScreenWidthDp < 600 &&
        configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val dialogHorizontalPadding = if (isPhonePortrait) 12.dp else 24.dp
    val dialogVerticalPadding = if (isPhonePortrait) 12.dp else 24.dp

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
        individual.primaryObjectId?.takeIf { it.isNotBlank() }?.let { add("Primary object" to it) }
    }

    val timelineEntries = individual.timeline

    val additionalNotes = individual.notes
        .mapNotNull { it.takeIf { text -> text.isNotBlank() } }

    val contentPadding = if (isPhonePortrait) 16.dp else 24.dp
    val sectionSpacing = if (isPhonePortrait) 16.dp else 24.dp
    val detailSpacing = if (isPhonePortrait) 12.dp else 16.dp
    val noteSpacing = if (isPhonePortrait) 6.dp else 8.dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dialogHorizontalPadding, vertical = dialogVerticalPadding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
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

                    val hasContent = detailItems.isNotEmpty() || timelineEntries.isNotEmpty() || additionalNotes.isNotEmpty()

                    Column(verticalArrangement = Arrangement.spacedBy(detailSpacing)) {
                        if (!hasContent) {
                            Text(
                                text = "No additional information available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            detailItems.forEach { (label, value) ->
                                DetailRow(label = label, value = value)
                            }
                        }
                    }

                    if (timelineEntries.isNotEmpty()) {
                        TimelineSection(
                            timeline = timelineEntries,
                            detailSpacing = detailSpacing,
                            noteSpacing = noteSpacing
                        )
                    }

                    if (additionalNotes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(noteSpacing)) {
                            Text(
                                text = "Additional notes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            additionalNotes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
private fun TimelineSection(
    timeline: List<TimelineEntry>,
    detailSpacing: Dp,
    noteSpacing: Dp
) {
    Column(verticalArrangement = Arrangement.spacedBy(detailSpacing)) {
        Text(
            text = "Timeline",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Column(verticalArrangement = Arrangement.spacedBy(detailSpacing)) {
            timeline.forEach { entry ->
                TimelineEventItem(entry = entry, noteSpacing = noteSpacing)
            }
        }
    }
}

@Composable
private fun TimelineEventItem(
    entry: TimelineEntry,
    noteSpacing: Dp
) {
    val event = entry.event
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val headline = buildString {
            append(entry.label)
            event.value?.takeIf { it.isNotBlank() }?.let {
                append(": ")
                append(it)
            }
        }
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        val keyDetails = mutableListOf<Pair<String, String>>()
        event.date?.takeIf { it.isNotBlank() }?.let { keyDetails.add("Date" to it) }
        event.place?.takeIf { it.isNotBlank() }?.let { keyDetails.add("Place" to it) }
        event.address?.takeIf { it.isNotBlank() }?.let { keyDetails.add("Address" to it) }

        keyDetails.forEach { (label, value) ->
            DetailRow(label = label, value = value)
        }

        event.details.forEach { (label, values) ->
            values.filter { it.isNotBlank() }.forEach { value ->
                DetailRow(label = label, value = value)
            }
        }

        if (event.notes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(noteSpacing)) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                event.notes.forEach { note ->
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
    stackVertically: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val header = if (children.isEmpty()) "Children" else "Children (${children.size})"
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (children.isEmpty()) {
            if (stackVertically) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
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
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
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
