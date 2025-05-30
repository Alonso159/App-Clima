package com.example.appclima.model

data class PronosticoResponse(
    val list: List<PronosticoDia>
)

data class PronosticoDia(
    val dt_txt: String,
    val main: MainPronostico,
    val weather: List<WeatherPronostico>
)

data class MainPronostico(
    val temp_min: Double,
    val temp_max: Double
)

data class WeatherPronostico(
    val description: String
)
