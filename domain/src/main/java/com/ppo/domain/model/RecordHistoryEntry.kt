package com.ppo.domain.model

import java.time.Instant

data class RecordHistoryEntry(
    val id: VersionId,
    val recordId: RecordId,
    val categoryId: CategoryId,
    val startTime: Instant,
    val endTime: Instant?,
    val description: String,
    val modifiedAt: Instant
)
