package com.oqba26.prayertimes.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.oqba26.prayertimes.models.UserNote
import com.oqba26.prayertimes.utils.DateConverter
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM user_notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<UserNote>>

    @Upsert
    suspend fun upsertNote(note: UserNote)

    @Query("DELETE FROM user_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    @Query("SELECT * FROM user_notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): UserNote?
}


@Database(entities = [UserNote::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prayer_times_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
