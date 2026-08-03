package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.examples.time_manager.R
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.ui.theme.arrowLeftIcon
import org.examples.time_manager.ui.theme.workIcon

@Composable
fun CategorySegments(
    projects: List<Project>,
    selectedProjects: List<Project>,
    onSelectProject: (Project) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    monthColors: MonthViewColors = MonthViewColors.defaults(),
) {
    if (projects.isEmpty()) {
        return
    }
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    val selectedCount = selectedProjects.size
    val clearEnabled = selectedCount > 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selectedCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.selected_projects),
                    style = texts.labelLarge.copy(
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                )
                SelectionCountBadge(
                    count = selectedCount,
                    monthColors = monthColors,
                )
                Spacer(modifier = Modifier.weight(1f))
                ClearSelectionButton(
                    enabled = clearEnabled,
                    onClick = onClearSelection,
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(projects.size, key = { projects[it].id }) { index ->
                val project = projects[index]
                val isSelected = selectedProjects.contains(project)
                val chipBorder =
                    if (isSelected) {
                        monthColors.accent.copy(alpha = 0.75f)
                    } else {
                        monthColors.border
                    }

                Surface(
                    modifier =
                        Modifier
                            .height(48.dp),
                    shape = RoundedCornerShape(25.dp),
                    color =
                        if (isSelected) {
                            monthColors.chipBackground
                        } else {
                            monthColors.chipBackground.copy(alpha = 0.72f)
                        },
                    border = BorderStroke(1.dp, chipBorder),
                    tonalElevation = 1.dp,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable { onSelectProject(project) }
                                .padding(horizontal = 14.dp),
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = workIcon(),
                                tint = colors.secondary,
                                contentDescription = ""
                            )
                            Text(
                                text = project.name,
                                style = texts.labelLarge.copy(
                                    color = colors.onSurface,
                                    fontWeight = FontWeight.Medium,
                                ),
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (isSelected) {
                            SelectedUnderline(
                                color = monthColors.accent,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    monthColors: MonthViewColors = MonthViewColors.defaults(),
) {
    val borderColor =
        if (count > 0) {
            monthColors.accent.copy(alpha = 0.65f)
        } else {
            monthColors.accentMuted.copy(alpha = 0.45f)
        }
    val backgroundColor =
        if (count > 0) {
            monthColors.chipBackground
        } else {
            monthColors.accent.copy(alpha = 0.08f)
        }
    val textColor = if (count > 0) monthColors.text else monthColors.accent

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 1.dp,
    ) {
        Text(
            text = count.toString(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ClearSelectionButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    val textColor = if (enabled) colors.secondary else colors.secondary.copy(alpha = .5f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .clickable(enabled = enabled) { onClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.reset),
                style = texts.labelLarge.copy(
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                )
            )
        }
    }
}
