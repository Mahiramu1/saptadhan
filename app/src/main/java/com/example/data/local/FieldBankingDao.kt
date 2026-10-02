package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Borrower
import com.example.data.model.CashReconciliation
import com.example.data.model.JlgGroup
import com.example.data.model.KendraCenter
import com.example.data.model.LoanApplication
import com.example.data.model.RepaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldBankingDao {

    // Kendra Centers
    @Query("SELECT * FROM kendra_centers ORDER BY meetingTime ASC")
    fun getAllCenters(): Flow<List<KendraCenter>>

    @Query("SELECT * FROM kendra_centers WHERE centerId = :centerId")
    suspend fun getCenterById(centerId: String): KendraCenter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenters(centers: List<KendraCenter>)

    @Update
    suspend fun updateCenter(center: KendraCenter)

    // JLG Groups
    @Query("SELECT * FROM jlg_groups ORDER BY groupId DESC")
    fun getAllGroups(): Flow<List<JlgGroup>>

    @Query("SELECT * FROM jlg_groups WHERE centerId = :centerId")
    fun getGroupsByCenter(centerId: String): Flow<List<JlgGroup>>

    @Query("SELECT * FROM jlg_groups WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: String): JlgGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: JlgGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<JlgGroup>)

    @Update
    suspend fun updateGroup(group: JlgGroup)

    // Borrowers
    @Query("SELECT * FROM borrowers ORDER BY fullName ASC")
    fun getAllBorrowers(): Flow<List<Borrower>>

    @Query("SELECT * FROM borrowers WHERE groupId = :groupId")
    fun getBorrowersByGroup(groupId: String): Flow<List<Borrower>>

    @Query("SELECT * FROM borrowers WHERE borrowerId = :borrowerId")
    suspend fun getBorrowerById(borrowerId: String): Borrower?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrower(borrower: Borrower)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrowers(borrowers: List<Borrower>)

    @Update
    suspend fun updateBorrower(borrower: Borrower)

    // Repayments
    @Query("SELECT * FROM repayment_records ORDER BY timestamp DESC")
    fun getAllRepayments(): Flow<List<RepaymentRecord>>

    @Query("SELECT * FROM repayment_records WHERE centerId = :centerId")
    fun getRepaymentsByCenter(centerId: String): Flow<List<RepaymentRecord>>

    @Query("SELECT * FROM repayment_records WHERE borrowerId = :borrowerId ORDER BY timestamp DESC")
    fun getRepaymentsByBorrower(borrowerId: String): Flow<List<RepaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: RepaymentRecord)

    // Unsynced count
    @Query("SELECT COUNT(*) FROM repayment_records WHERE syncPending = 1")
    fun getPendingRepaymentsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM borrowers WHERE syncPending = 1")
    fun getPendingBorrowersCount(): Flow<Int>

    @Query("UPDATE repayment_records SET syncPending = 0")
    suspend fun markAllRepaymentsSynced()

    @Query("UPDATE borrowers SET syncPending = 0")
    suspend fun markAllBorrowersSynced()

    @Query("UPDATE jlg_groups SET syncPending = 0")
    suspend fun markAllGroupsSynced()

    // Cash Reconciliation
    @Query("SELECT * FROM cash_reconciliation WHERE reconciliationDate = :date LIMIT 1")
    fun getReconciliationForDate(date: String): Flow<CashReconciliation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReconciliation(reconciliation: CashReconciliation)

    // Loan Applications
    @Query("SELECT * FROM loan_applications ORDER BY timestamp DESC")
    fun getAllLoanApplications(): Flow<List<LoanApplication>>

    @Query("SELECT * FROM loan_applications WHERE applicationId = :id")
    suspend fun getLoanApplicationById(id: String): LoanApplication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanApplication(application: LoanApplication)

    @Update
    suspend fun updateLoanApplication(application: LoanApplication)
}
