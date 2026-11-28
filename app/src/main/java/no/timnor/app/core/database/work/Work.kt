package no.timnor.app.core.database.work

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class Work(
    val description: String = "",
    val date: LocalDateTime,
    val project: Int,
    val task: Int = 0,
    val time: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appliedHourlyRateOre: Int? = null,
    val enteredEndMinute: Int? = null,
)
