package no.timnor.app.core.database.project

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Project(
    val description: String = "",
    val name: String = "",
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hourlyRateOre: Int? = null,
    val weekendHourlyRateOre: Int? = null,
)
