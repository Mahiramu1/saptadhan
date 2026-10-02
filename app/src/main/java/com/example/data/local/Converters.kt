package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AttendanceStatus
import com.example.data.model.MandalCluster
import com.example.data.model.OverdueReason
import com.example.data.model.PaymentMode

class Converters {
    @TypeConverter
    fun fromMandal(mandal: MandalCluster?): String? = mandal?.name

    @TypeConverter
    fun toMandal(value: String?): MandalCluster? = value?.let {
        try { MandalCluster.valueOf(it) } catch (e: Exception) { MandalCluster.KOTHAKOTA }
    }

    @TypeConverter
    fun fromAttendance(status: AttendanceStatus?): String? = status?.name

    @TypeConverter
    fun toAttendance(value: String?): AttendanceStatus? = value?.let {
        try { AttendanceStatus.valueOf(it) } catch (e: Exception) { AttendanceStatus.PRESENT }
    }

    @TypeConverter
    fun fromPaymentMode(mode: PaymentMode?): String? = mode?.name

    @TypeConverter
    fun toPaymentMode(value: String?): PaymentMode? = value?.let {
        try { PaymentMode.valueOf(it) } catch (e: Exception) { PaymentMode.CASH }
    }

    @TypeConverter
    fun fromOverdueReason(reason: OverdueReason?): String? = reason?.name

    @TypeConverter
    fun toOverdueReason(value: String?): OverdueReason? = value?.let {
        try { OverdueReason.valueOf(it) } catch (e: Exception) { null }
    }
}
