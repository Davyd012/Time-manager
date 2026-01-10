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

@Composable
fun CategorySegments(
    projects: List<Project>,
    selectedProjects: List<Project>,
    onSelectProject: (Project) -> Unit,
    onClearSelection: () -> Unit,
    colors: MonthViewColors,
    modifier: Modifier = Modifier,
) {
    if (projects.isEmpty()) {
        return
    }

    val selectedCount = selectedProjects.size
    val clearEnabled = selectedCount > 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.selected_projects),
                color = colors.mutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            SelectionCountBadge(
                count = selectedCount,
                colors = colors,
            )
            Spacer(modifier = Modifier.weight(1f))
            ClearSelectionButton(
                enabled = clearEnabled,
                onClick = onClearSelection,
                colors = colors,
            )
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
                        colors.accent.copy(alpha = 0.7f)
                    } else {
                        colors.border.copy(alpha = 0.9f)
                    }

                Surface(
                    modifier =
                        Modifier
                            .height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    color =
                        if (isSelected) {
                            colors.cardBackground.copy(alpha = 0.95f)
                        } else {
                            colors.cardBackground
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
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isSelected) colors.accent else colors.border,
                                            CircleShape
                                        ),
                            )
                            Text(
                                text = project.name,
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (isSelected) {
                            SelectedUnderline(
                                color = colors.accent,
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
    colors: MonthViewColors,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (count > 0) {
            colors.accent.copy(alpha = 0.6f)
        } else {
            colors.border.copy(alpha = 0.8f)
        }
    val backgroundColor =
        if (count > 0) {
            colors.accent.copy(alpha = 0.15f)
        } else {
            colors.cardBackground
        }
    val textColor = if (count > 0) colors.accent else colors.mutedText

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
    colors: MonthViewColors,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (enabled) {
            colors.accent.copy(alpha = 0.7f)
        } else {
            colors.border.copy(alpha = 0.6f)
        }
    val backgroundColor =
        if (enabled) {
            colors.cardBackground
        } else {
            colors.cardBackground.copy(alpha = 0.6f)
        }
    val textColor = if (enabled) colors.accent else colors.mutedText

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
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
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
