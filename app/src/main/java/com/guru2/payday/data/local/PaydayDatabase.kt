package com.guru2.payday.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ExpenseEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PaydayDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: PaydayDatabase? = null

        fun getInstance(context: Context): PaydayDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PaydayDatabase::class.java,
                    "payday.db",
                ).build().also { instance = it }
            }
    }
}
