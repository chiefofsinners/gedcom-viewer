package com.lewisdeveloping.gedcomviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.ui.theme.Lavender

@Composable
fun PersonCard(
    individual: Individual?,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: ((String) -> Unit)? = null
) {
    val clickableModifier = if (individual != null && onClick != null) {
        modifier.clickable { onClick(individual.id) }
    } else {
        modifier
    }

    Card(
        modifier = clickableModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(initials = individual?.displayName?.initials().orEmpty())
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = individual?.displayName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    individual?.birth?.description()?.let { description ->
                        Text(
                            text = "Born: $description",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    individual?.death?.description()?.let { description ->
                        Text(
                            text = "Died: $description",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonRow(
    individual: Individual?,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onClick: ((String) -> Unit)? = null
) {
    val clickableModifier = if (individual != null && onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable { onClick(individual.id) }
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = clickableModifier.padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(initials = individual?.displayName?.initials().orEmpty(), size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = individual?.displayName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Avatar(initials: String, size: Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Lavender),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (initials.isNotBlank()) initials else "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun String.initials(): String =
    trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }
