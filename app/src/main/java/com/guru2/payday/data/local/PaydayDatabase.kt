package com.guru2.payday.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserEntity::class, ExpenseEntity::class, IncomeEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PaydayDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var instance: PaydayDatabase? = null

        fun getInstance(context: Context): PaydayDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PaydayDatabase::class.java,
                    "payday.db",
                )
                    // 새 설치에서는 제출용 데모 계정과 예시 거래가 포함된 초기 DB를 복사한다.
                    .createFromAsset("database/payday-demo.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `users` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`email` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, " +
                        "`passwordSalt` TEXT NOT NULL, `nickname` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `incomes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`userId` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`amount` INTEGER NOT NULL, `category` TEXT NOT NULL, " +
                        "`receivedDate` TEXT NOT NULL, `memo` TEXT NOT NULL, " +
                        "`isRecurring` INTEGER NOT NULL, `recurrence` TEXT, " +
                        "`updatedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `userId` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
