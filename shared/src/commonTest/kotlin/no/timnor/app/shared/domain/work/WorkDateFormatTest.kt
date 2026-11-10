package no.timnor.app.shared.domain.work

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkDateFormatTest {
    @Test
    fun formatsIsoWorkDateLikeAndroidEditor() {
        assertEquals("09-11-2026", formatWorkDateInput("2026-11-09"))
    }

    @Test
    fun preservesInvalidInputForCorrection() {
        assertEquals("invalid", formatWorkDateInput("invalid"))
    }
}
