package com.lewisdeveloping.gedcomviewer.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.TimelineEntry
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualDetailsDialog(
    individual: Individual,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val themeColors = AppTheme.colors
    val maxDialogHeight = configuration.screenHeightDp.dp * 0.9f
    val scrollState = rememberScrollState()
    val isPhonePortrait = configuration.smallestScreenWidthDp < 600 &&
        configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val dialogHorizontalPadding = if (isPhonePortrait) 12.dp else 24.dp
    val dialogVerticalPadding = if (isPhonePortrait) 12.dp else 24.dp
    val contentPadding = if (isPhonePortrait) 16.dp else 24.dp
    val sectionSpacing = if (isPhonePortrait) 16.dp else 24.dp
    val detailSpacing = if (isPhonePortrait) 12.dp else 16.dp
    val noteSpacing = if (isPhonePortrait) 6.dp else 8.dp

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
    val additionalNotes = individual.notes.mapNotNull { it.takeIf { text -> text.isNotBlank() } }
    val hasContent = detailItems.isNotEmpty() || timelineEntries.isNotEmpty() || additionalNotes.isNotEmpty()

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
                color = themeColors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding)
                        .heightIn(max = maxDialogHeight),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                ) {
                    HeaderSection()

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
                    ) {
                        if (!hasContent) {
                                Text(
                                    text = "No additional information available.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = themeColors.supportingText
                                )
                        } else {
                            if (detailItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(detailSpacing)) {
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
                                AdditionalNotesSection(
                                    notes = additionalNotes,
                                    noteSpacing = noteSpacing
                                )
                            }
                        }
                    }

                    FooterSection(onDismissRequest = onDismissRequest)
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = AppTheme.colors.accent
        )
        Text(
            text = "Individual details",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun FooterSection(onDismissRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismissRequest) {
            Text(text = "Close")
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
            color = AppTheme.colors.infoForeground
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
                    color = AppTheme.colors.supportingText
                )
                event.notes.forEach { note ->
                    Text(
                        text = "• $note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.infoForeground
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalNotesSection(
    notes: List<String>,
    noteSpacing: Dp
) {
    Column(verticalArrangement = Arrangement.spacedBy(noteSpacing)) {
        Text(
            text = "Additional notes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        notes.forEach { note ->
            Text(
                text = "• $note",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.infoForeground
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.supportingText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.infoForeground
        )
    }
}
