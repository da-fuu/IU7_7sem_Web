package com.ppo.domain.model

import java.time.Instant

data class DescriptionVersion(
    val id: VersionId,
    val recordId: RecordId,
    val description: String,
    val modifiedAt: Instant
)
