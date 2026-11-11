package no.timnor.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import no.timnor.app.shared.navigation.SharedAppTab
import no.timnor.app.shared.navigation.SharedNavigationBar
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SharedNavigationBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tabsExposeSelectionAndUseLegacyMinimumTouchHeight() {
        var selectedTab by mutableStateOf(SharedAppTab.Home)
        composeRule.setContent {
            TimnorTheme(darkTheme = false) {
                SharedNavigationBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                )
            }
        }

        val homeTab = composeRule.onNodeWithText("Hjem")
        homeTab.assertIsSelected()
        val itemHeightDp = with(composeRule.density) {
            homeTab.fetchSemanticsNode().boundsInRoot.height.toDp().value
        }
        assertTrue("navigation tab should meet the legacy 56 dp minimum", itemHeightDp >= 56f)

        val stopwatchTab = composeRule.onNodeWithText("Stoppeklokke")
        stopwatchTab.assertIsNotSelected()
        stopwatchTab.performClick()
        composeRule.runOnIdle { assertTrue(selectedTab == SharedAppTab.Stopwatch) }
        stopwatchTab.assertIsSelected()
    }
}
