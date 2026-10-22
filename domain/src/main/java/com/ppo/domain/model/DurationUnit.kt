package com.ppo.domain.model

enum class DurationUnit {
    seconds,
    minutes,
    hours,
    days;

    fun toSeconds(value: Long): Long = when (this) {
        seconds -> value
        minutes -> value * 60L
        hours -> value * 3600L
        days -> value * 86400L
    }
}
