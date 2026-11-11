package no.timnor.app.shared.features.root.presentation.home

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import no.timnor.app.shared.features.root.presentation.home.components.homeDailyTimelineShape

class HomeDailyWorkListShapeTest {
    @Test
    fun homeTimelineKeepsThemeTopCornersAndSquaresOnlyTheBottom() {
        val themeShape = RoundedCornerShape(28.dp)

        assertEquals(
            themeShape.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
            homeDailyTimelineShape(themeShape),
        )
    }
}
