package com.lewisdeveloping.gedcomviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.model.Individual

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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

    Column(
        modifier = clickableModifier.padding(vertical = 12.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
