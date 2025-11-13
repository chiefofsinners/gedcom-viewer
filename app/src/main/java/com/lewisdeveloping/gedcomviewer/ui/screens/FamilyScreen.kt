package com.lewisdeveloping.gedcomviewer.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanel
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanelStyle
import com.lewisdeveloping.gedcomviewer.ui.components.PersonCard
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme

@Composable
fun FamilyScreen(
    individualId: String,
    data: GedcomData,
    onIndividualSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val focus = data.individuals[individualId]
    val baseModifier = modifier
        .fillMaxSize()
        .background(colors.background)

    if (focus == null) {
        Column(
            modifier = baseModifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoPanel(
                text = "Individual not found",
                style = InfoPanelStyle.Error,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val partnerFamily = findFamily(focus.familiesAsSpouse, data)
    val partner = partnerFamily?.partnerFor(focus, data)
    val focusParents = findFamily(focus.familiesAsChild, data)
    val children = partnerFamily?.childrenIds?.mapNotNull { data.individuals[it] } ?: emptyList()

    BoxWithConstraints(
        modifier = baseModifier
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
                isCompact = isCompact,
                usePanelBackground = isTablet
            )

            FamilyCoreSection(
                focus = focus,
                partner = partner,
                marriage = partnerFamily?.marriage,
                onIndividualSelected = onIndividualSelected,
                isCompact = isCompact,
                usePanelBackground = isTablet
            )

            ChildrenSection(
                children = children,
                onIndividualSelected = onIndividualSelected,
                stackVertically = stackChildrenVertically,
                isCompact = isCompact,
                usePanelBackground = isTablet
            )
        }
    }
}
@Composable
private fun ParentsSection(
    focusParents: Family?,
    data: GedcomData,
    onIndividualSelected: (String) -> Unit,
    isCompact: Boolean,
    usePanelBackground: Boolean
) {
    SectionContainer(usePanelBackground = usePanelBackground) {
        Text(
            text = "Parents",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (focusParents == null) {
            InfoPanel(
                text = "No recorded parents.",
                style = InfoPanelStyle.Surface,
                modifier = (if (isCompact) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxWidth(0.5f)
                }).align(Alignment.Start)
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
    isCompact: Boolean,
    usePanelBackground: Boolean
) {
    val colors = AppTheme.colors
    SectionContainer(usePanelBackground = usePanelBackground) {
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
                    label = "Spouse",
                    modifier = modifier,
                    onClick = onIndividualSelected
                )
            }
        )
        marriage?.description()?.let { description ->
            Card(
                modifier = Modifier.align(Alignment.Start),
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
    stackVertically: Boolean,
    isCompact: Boolean,
    usePanelBackground: Boolean
) {
    SectionContainer(usePanelBackground = usePanelBackground) {
        val header = if (children.isEmpty()) "Children" else "Children (${children.size})"
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (children.isEmpty()) {
            InfoPanel(
                text = "No recorded children",
                style = InfoPanelStyle.Surface,
                modifier = (if (isCompact) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxWidth(0.5f)
                }).align(Alignment.Start)
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

@Composable
private fun SectionContainer(
    usePanelBackground: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    if (usePanelBackground) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colors.tabBackground,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
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
