package com.example.appclima.model

data class ClimaResponse(
    val name: String,
    val main: Main,
    val weather: List<Clima>
)

data class Main(
    val temp: Float,
    val temp_min: Float,
    val temp_max: Float
)

data class Clima(
    val description: String,
    val icon: String
)