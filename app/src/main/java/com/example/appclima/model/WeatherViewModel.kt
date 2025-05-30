package com.example.appclima.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appclima.network.RetrofitClient
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val apiKey = "fc2aace935eded5531aa5903b3e7cffd" // Api Key
    var clima by mutableStateOf<ClimaResponse?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set


    var pronostico by mutableStateOf<List<PronosticoDia>>(emptyList())
        private set


    fun obtenerPronostico(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.instance.getForecastByCoords(lat, lon, apiKey)
                pronostico = respuesta.list
            } catch (e: Exception) {
                error = "Error al obtener el pronóstico"
            }
        }
    }

    fun obtenerClimaPorCoordenadas(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.instance.getWeatherByCoordinates(
                    lat,
                    lon,
                    apiKey,
                    "metric",
                    "es"
                )
                clima = respuesta
                error = null
            } catch (e: Exception) {
                error = "Error al obtener el clima"
                clima = null
            }
        }
    }
}


