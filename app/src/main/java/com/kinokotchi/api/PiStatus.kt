package com.kinokotchi.api

data class PiStatus(
    val light: Int,
    val fan: Int,
    val moisture: Double,
    val isFoodLow: Boolean,
    val temperature: Double,
    val readyToHarvest: Boolean,
    val planted: Boolean
)