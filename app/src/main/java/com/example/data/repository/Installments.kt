package com.example.data.repository

import com.example.data.model.JlgGroup
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class Installments(val start: Long, val daysElapsed: Long, val count: Int, val amount: Double, val term: Int) {
    val totalDue get() = count * amount
    fun installmentDate(paid: Double): String? {
        val index = (paid / amount).toLong() + 1
        if (index > term) return null
        return Instant.ofEpochMilli(start).atZone(zone).toLocalDate().plusDays(index * 14).toString()
    }
    fun daysOverdue(paid: Double): Int {
        if (amount <= 0 || paid >= totalDue) return 0
        val firstUnpaid = (paid / amount).toInt() + 1
        return (daysElapsed - firstUnpaid * 14L).coerceAtLeast(0).toInt()
    }
    companion object {
        private val zone = ZoneId.of("Asia/Kolkata")
        fun forGroup(group: JlgGroup, now: Long): Installments? {
            if (!group.isDisbursed || !group.isSanctioned) return null
            val start = LocalDate.parse(group.disbursedOn ?: return null)
            val elapsed = ChronoUnit.DAYS.between(start, Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
            val kfs = FieldBankingRepository.calculateKfs("", group.groupName, group.sanctionedAmountPerMember, group.loanCycle)
            val count = (elapsed / 14).coerceAtMost(kfs.numberOfInstallments.toLong()).toInt()
            if (count < 1) return null
            return Installments(start.atStartOfDay(zone).toInstant().toEpochMilli(), elapsed, count, kfs.installmentAmount, kfs.numberOfInstallments)
        }
    }
}
