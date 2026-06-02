package com.ppo.domain.model

import java.util.UUID

@JvmInline
value class UserId(val value: UUID)

@JvmInline
value class CategoryId(val value: UUID)

@JvmInline
value class RecordId(val value: UUID)

@JvmInline
value class VersionId(val value: UUID)
