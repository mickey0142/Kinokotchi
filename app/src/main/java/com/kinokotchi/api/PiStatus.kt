package com.kinokotchi.api

data class PiStatus(
    val light: Int,
    val fan: Int,
    val moisture: Double,
    val foodLevel: Boolean,
    val temperature: Double,
    val growth: Int
)