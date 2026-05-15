package com.ppo.domain.model

import java.time.Instant
import java.time.Duration

data class TimeRecord(
    val id: RecordId,
    val userId: UserId,
    val categoryId: CategoryId,
    val startTime: Instant,
    val endTime: Instant? = null,
    val description: String = ""
) {
    fun getDurationMinutes(): Long {
        return endTime?.let {
            Duration.between(startTime, it).toMinutes()
        } ?: 0L
    }
}
