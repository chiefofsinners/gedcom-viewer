package com.lewisdeveloping.gedcomviewer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lewisdeveloping.gedcomviewer.R
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBar
import com.lewisdeveloping.gedcomviewer.ui.components.FileActionBarSelection
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanel
import com.lewisdeveloping.gedcomviewer.ui.components.InfoPanelStyle
import com.lewisdeveloping.gedcomviewer.ui.components.PersonRow
import com.lewisdeveloping.gedcomviewer.ui.theme.AppTheme
import com.lewisdeveloping.gedcomviewer.ui.theme.AppThemeOption
import com.lewisdeveloping.gedcomviewer.ui.theme.GedcomViewerTheme
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualsScreen(
    individuals: List<Individual>,
    currentFileName: String?,
    onNavigateHome: () -> Unit,
    onNavigateIndex: () -> Unit,
    onNavigateFamily: () -> Unit,
    familyEnabled: Boolean,
    onIndividualSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    listState: LazyListState
) {
    val colors = AppTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val appTitle = stringResource(id = R.string.app_name)
    val displayTitle = remember(currentFileName) {
        currentFileName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { name ->
                if (name.lowercase(Locale.getDefault()).endsWith(".ged")) {
                    name.dropLast(4)
                } else {
                    name
                }
            }
    } ?: appTitle

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
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = displayTitle
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    scrolledContainerColor = colors.background,
                    titleContentColor = colors.supportingText,
                    navigationIconContentColor = colors.supportingText,
                    actionIconContentColor = colors.supportingText
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        bottomBar = {
            FileActionBar(
                selected = FileActionBarSelection.INDEX,
                onNavigateHome = onNavigateHome,
                onNavigateIndex = onNavigateIndex,
                onNavigateFamily = onNavigateFamily,
                familyEnabled = familyEnabled
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
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text(text = "Search by name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            coroutineScope.launch {
                                listState.scrollToItem(0)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    disabledContainerColor = colors.surface,
                    focusedBorderColor = colors.supportingText.copy(alpha = 0.6f),
                    unfocusedBorderColor = colors.supportingText.copy(alpha = 0.4f),
                    focusedLeadingIconColor = colors.supportingText,
                    unfocusedLeadingIconColor = colors.supportingText,
                    focusedTrailingIconColor = colors.supportingText,
                    unfocusedTrailingIconColor = colors.supportingText,
                    cursorColor = colors.supportingText
                )
            )

            val availableSections = sections.takeIf { it.size > 1 } ?: emptyList()
            val indexLetters = remember(availableSections) {
                availableSections.map { it.title }.distinct().sorted()
            }

            if (sections.isEmpty()) {
                InfoPanel(
                    text = "No individuals found",
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    style = InfoPanelStyle.Info
                )
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        sections.forEach { section ->
                            item(key = "header-${section.title}") {
                                Surface(
                                    color = colors.background,
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = section.title.toString(),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            itemsIndexed(section.members, key = { _, individual -> individual.id }) { index, individual ->
                                PersonRow(
                                    individual = individual,
                                    supportingText = individual.birth?.description(),
                                    onClick = onIndividualSelected
                                )
                                if (index < section.members.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = colors.border.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                    if (indexLetters.isNotEmpty()) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(56.dp)
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surface)
                                .padding(vertical = 16.dp)
                        ) {
                            val letterDividerSpacing = 12.dp
                            val condensedLetters = remember(indexLetters, maxHeight) {
                                condenseLetters(
                                    letters = indexLetters,
                                    availableHeight = maxHeight,
                                    minLetterHeight = 28.dp,
                                    dividerSpacing = letterDividerSpacing
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = if (condensedLetters.size > 1) Arrangement.SpaceEvenly else Arrangement.Center
                            ) {
                                condensedLetters.forEachIndexed { index, letter ->
                                    LetterIndexEntry(
                                        letter = letter,
                                        onClick = {
                                            letterPositions[letter]?.let { targetIndex ->
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(targetIndex)
                                                }
                                            }
                                        }
                                    )
                                    if (index != condensedLetters.lastIndex) {
                                        Spacer(modifier = Modifier.height(letterDividerSpacing / 2))
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            thickness = 0.5.dp,
                                            color = colors.infoForeground.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(letterDividerSpacing / 2))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 640)
@Composable
private fun IndividualsScreenPreview() {
    val individuals = listOf(
        Individual(
            id = "preview::I1",
            gedcomId = "I1",
            sourceId = "preview",
            fullName = "Anthony Edward Munro",
            givenName = "Anthony",
            surname = "Munro",
            gender = Individual.Gender.MALE,
            birth = LifeEvent(date = "12 JAN 1922", place = "Cheltenham, Gloucestershire, England")
        ),
        Individual(
            id = "preview::I2",
            gedcomId = "I2",
            sourceId = "preview",
            fullName = "Julia Amanda Fish",
            givenName = "Julia",
            surname = "Fish",
            gender = Individual.Gender.FEMALE,
            birth = LifeEvent(date = "12 JAN 1925", place = "Greenwich, London, England")
        ),
        Individual(
            id = "preview::I3",
            gedcomId = "I3",
            sourceId = "preview",
            fullName = "Nigel Raymond Munro",
            givenName = "Nigel",
            surname = "Munro",
            gender = Individual.Gender.MALE
        )
    )

    val previewListState = rememberLazyListState()

    GedcomViewerTheme(theme = AppThemeOption.SILVER) {
        IndividualsScreen(
            individuals = individuals,
            currentFileName = "Sample-GEDCOM.ged",
            onNavigateHome = {},
            onNavigateIndex = {},
            onNavigateFamily = {},
            familyEnabled = false,
            onIndividualSelected = {},
            searchQuery = "",
            onSearchQueryChange = {},
            listState = previewListState
        )
    }
}
private data class IndividualSection(val title: Char, val members: List<Individual>)

private fun buildSections(individuals: List<Individual>): List<IndividualSection> {
    if (individuals.isEmpty()) return emptyList()

    val locale = Locale.getDefault()
    val sortedIndividuals = individuals.sortedWith(
        compareBy<Individual>(
            { it.indexLetter() },
            {
                it.surname?.takeIf(String::isNotBlank)?.lowercase(locale)
                    ?: it.displayName.lowercase(locale)
            },
            { it.givenName?.lowercase(locale) ?: "" },
            { it.displayName.lowercase(locale) },
            { it.id }
        )
    )

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

    for (individual in sortedIndividuals) {
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

@Composable
private fun LetterIndexEntry(
    letter: Char,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.infoForeground
        )
    }
}

private fun condenseLetters(
    letters: List<Char>,
    availableHeight: Dp,
    minLetterHeight: Dp,
    dividerSpacing: Dp
): List<Char> {
    if (letters.isEmpty()) return emptyList()
    val usableHeight = availableHeight.coerceAtLeast(0.dp).value
    if (usableHeight <= 0f) return letters
    val minHeightPx = minLetterHeight.coerceAtLeast(0.dp).value
    val dividerPx = dividerSpacing.coerceAtLeast(0.dp).value

    fun letterHeight(count: Int): Float {
        if (count <= 0) return 0f
        val dividerTotal = dividerPx * (count - 1).coerceAtLeast(0)
        val letterSpace = (usableHeight - dividerTotal).coerceAtLeast(0f)
        return if (count > 0) letterSpace / count else 0f
    }

    if (letterHeight(letters.size) >= minHeightPx) return letters

    var stride = 2
    var best = letters
    val lastIndex = letters.lastIndex

    while (stride <= letters.size) {
        val candidate = letters.filterIndexed { index, _ ->
            index == 0 || index == lastIndex || index % stride == 0
        }
        best = candidate
        if (candidate.size <= 2 || letterHeight(candidate.size) >= minHeightPx) break
        stride += 1
    }

    return best
}
