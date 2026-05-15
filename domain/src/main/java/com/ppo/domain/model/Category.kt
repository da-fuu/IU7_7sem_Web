package com.ppo.domain.model

data class Category(
    val id: CategoryId,
    val userId: UserId?,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean
)
