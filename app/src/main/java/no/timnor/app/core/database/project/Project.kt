package no.timnor.app.core.database.project

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class Project(
    val description: String = "",
    val name: String = "",
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)