package no.timnor.app.core.database

import android.content.Context
import androidx.room.Room

fun getDatabase(context: Context): Database =
    Room.databaseBuilder<Database>(
        context.applicationContext,
        context.getDatabasePath("work.db").absolutePath,
    ).build()
