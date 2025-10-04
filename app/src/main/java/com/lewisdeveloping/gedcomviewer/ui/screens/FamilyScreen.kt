package com.lewisdeveloping.gedcomviewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import com.lewisdeveloping.gedcomviewer.ui.components.PersonCard
import com.lewisdeveloping.gedcomviewer.ui.components.PersonRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    individualId: String,
    data: GedcomData,
    onNavigateBack: () -> Unit,
    onIndividualSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = data.individuals[individualId]

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = focus?.displayName ?: "Family") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            val partner = partnerFamily?.let { it.partnerFor(focus, data) }
            val focusParents = findFamily(focus.familiesAsChild, data)
            val partnerParents = partner?.let { findFamily(it.familiesAsChild, data) }
            val children = partnerFamily?.childrenIds?.mapNotNull { data.individuals[it] } ?: emptyList()

            BoxWithConstraints(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            ) {
                val isCompact = maxWidth < 600.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ParentsSection(
                        focusParents = focusParents,
                        partnerParents = partnerParents,
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
                        onIndividualSelected = onIndividualSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentsSection(
    focusParents: Family?,
    partnerParents: Family?,
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
        partnerParents?.let {
            Text(
                text = "Spouse's Parents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ResponsivePersonCardRow(
                isCompact = isCompact,
                first = { modifier ->
                    PersonCard(
                        individual = it.husbandId?.let { id -> data.individuals[id] },
                        label = "Father-in-law",
                        modifier = modifier,
                        onClick = onIndividualSelected
                    )
                },
                second = { modifier ->
                    PersonCard(
                        individual = it.wifeId?.let { id -> data.individuals[id] },
                        label = "Mother-in-law",
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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
    onIndividualSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val header = if (children.isEmpty()) "Children" else "Children (${children.size})"
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (children.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    text = "No recorded children",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card {
                Column {
                    children.forEach { child ->
                        PersonRow(
                            individual = child,
                            supportingText = child.birth?.description(),
                            onClick = onIndividualSelected
                        )
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
