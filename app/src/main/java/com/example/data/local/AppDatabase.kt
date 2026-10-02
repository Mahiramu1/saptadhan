package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Borrower
import com.example.data.model.CashReconciliation
import com.example.data.model.JlgGroup
import com.example.data.model.KendraCenter
import com.example.data.model.LoanApplication
import com.example.data.model.RepaymentRecord

@Database(
    entities = [
        KendraCenter::class,
        JlgGroup::class,
        Borrower::class,
        RepaymentRecord::class,
        CashReconciliation::class,
        LoanApplication::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fieldBankingDao(): FieldBankingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "disrub_collect_wanaparthy.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
