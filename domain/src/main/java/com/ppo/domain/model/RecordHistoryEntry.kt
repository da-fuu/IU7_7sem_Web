package com.ppo.domain.model

import java.time.Instant
import java.util.UUID

data class RecordHistoryEntry(
    val id: VersionId,
    val recordId: RecordId,
    val categoryId: CategoryId = CategoryId(UUID(0L, 0L)),
    val startTime: Instant = Instant.EPOCH,
    val endTime: Instant? = null,
    val description: String = "",
    val modifiedAt: Instant = Instant.now()
)
