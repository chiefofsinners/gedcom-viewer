package com.lewisdeveloping.gedcomviewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.R
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBarSelection
import com.lewisdeveloping.gedcomviewer.ui.components.PersonRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualsScreen(
    individuals: List<Individual>,
    currentFileName: String?,
    onOpenFile: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onIndividualSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredIndividuals = remember(individuals, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            individuals
        } else {
            individuals.filter { individual ->
                val displayName = individual.displayName.lowercase()
                val surname = individual.surname?.lowercase()
                val given = individual.givenName?.lowercase()
                displayName.contains(query) ||
                    (surname != null && surname.contains(query)) ||
                    (given != null && given.contains(query))
            }
        }
    }

    val sections = remember(filteredIndividuals) {
        buildSections(filteredIndividuals)
    }

    val letterPositions = remember(sections) {
        val mapping = mutableMapOf<Char, Int>()
        var index = 0
        sections.forEach { section ->
            mapping[section.title] = index
            index += 1 + section.members.size
        }
        mapping
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = currentFileName?.ifBlank { null } ?: stringResource(id = R.string.app_name))
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
                selected = FileActionBarSelection.INDEX,
                onNavigateHome = onNavigateHome,
                onNavigateIndex = onNavigateIndex,
                onOpenFile = onOpenFile
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text(text = "Search by name") },
                singleLine = true
            )

            val availableSections = sections.takeIf { it.size > 1 } ?: emptyList()
            if (availableSections.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(availableSections) { section ->
                        TextButton(
                            onClick = {
                                letterPositions[section.title]?.let { targetIndex ->
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            }
                        ) {
                            Text(text = section.title.toString())
                        }
                    }
                }
            }

            if (sections.isEmpty()) {
                Text(
                    text = "No individuals found",
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    sections.forEach { section ->
                        item(key = "header-${section.title}") {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = section.title.toString(),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        items(section.members, key = { it.id }) { individual ->
                            PersonRow(
                                individual = individual,
                                supportingText = individual.birth?.description(),
                                onClick = onIndividualSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class IndividualSection(val title: Char, val members: List<Individual>)

private fun buildSections(individuals: List<Individual>): List<IndividualSection> {
    if (individuals.isEmpty()) return emptyList()

    val sections = mutableListOf<IndividualSection>()
    var currentTitle: Char? = null
    val currentMembers = mutableListOf<Individual>()

    fun flushSection() {
        val title = currentTitle ?: return
        if (currentMembers.isNotEmpty()) {
            sections += IndividualSection(title, currentMembers.toList())
            currentMembers.clear()
        }
    }

    for (individual in individuals) {
        val letter = individual.indexLetter()
        if (letter != currentTitle) {
            flushSection()
            currentTitle = letter
        }
        currentMembers += individual
    }
    flushSection()

    return sections
}

private fun Individual.indexLetter(): Char {
    val nameSource = surname?.takeIf { it.isNotBlank() } ?: displayName
    val firstLetter = nameSource.firstOrNull { it.isLetter() } ?: '#'
    return firstLetter.uppercaseChar()
}
