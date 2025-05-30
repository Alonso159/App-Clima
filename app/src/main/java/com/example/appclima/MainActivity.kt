package com.example.appclima

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appclima.model.ClimaResponse
import com.example.appclima.model.PronosticoDia
import com.example.appclima.model.WeatherViewModel
import com.example.appclima.network.LocationService
import com.example.appclima.ui.theme.AppClimaTheme
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {


    private val viewModel: WeatherViewModel by viewModels()

    // Estado global para controlar si el permiso fue otorgado
    private val permisoOtorgado = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launcher moderno para pedir permisos
        val locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permisoOtorgado.value = isGranted
            if (isGranted) {
                obtenerUbicacionInicial()
            } else {
                mostrarDialogoIrAConfiguracion()
            }
        }

        // Verificación inicial
        if (!tienePermisos()) {
            mostrarDialogoExplicativo {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            permisoOtorgado.value = true
            obtenerUbicacionInicial()
        }

        setContent {
            AppClimaTheme {
                if (permisoOtorgado.value) {
                    PantallaClima()
                } else {
                    PantallaSinPermiso(
                        onIntentarVerificarPermiso = {
                            if (tienePermisos()) {
                                permisoOtorgado.value = true
                                obtenerUbicacionInicial()
                            } else {
                                Toast.makeText(this, "Permiso aún no concedido", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun tienePermisos(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    //Dialogo antes de pedir permisos
    private fun mostrarDialogoExplicativo(onPermisoAceptar: () -> Unit) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Permiso de ubicación")
        builder.setMessage("La aplicación necesita acceso a tu ubicación para mostrar el clima local.")
        builder.setCancelable(false)
        builder.setPositiveButton("Permitir") { _, _ ->
            onPermisoAceptar()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun mostrarDialogoIrAConfiguracion() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Permiso necesario")
        builder.setMessage("Has denegado el permiso y no se puede solicitar más. Por favor, habilítalo manualmente en la configuración.")
        builder.setCancelable(false)
        builder.setPositiveButton("Abrir Configuración") { _, _ ->
            // Abrir la configuración de la app
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("Salir") { _, _ ->
            finish()
        }
        builder.show()
    }

    private fun obtenerUbicacionInicial() {
        val locationService = LocationService(this)

        lifecycleScope.launch {
            try {
                val location = locationService.getCurrentLocationSuspend(this@MainActivity)
                Log.d("Ubicacion", "Ubicación obtenida: $location")
                location?.let {
                    //  Usa las coordenadas REALES
                    viewModel.obtenerClimaPorCoordenadas(it.latitude, it.longitude)
                    viewModel.obtenerPronostico(it.latitude, it.longitude)
                } ?: run {
                    // Solo si no hay ubicación disponible

                }
            } catch (e: Exception) {
                // Error al obtener ubicación → fallback a Lima

            }
        }
    }
}

fun obtenerIconoClima(descripcion: String): String {
    return when {
        descripcion.contains("muy nuboso", ignoreCase = true) -> "☁️"
        descripcion.contains("lluvia ligera", ignoreCase = true) -> "🌧️"
        descripcion.contains("lluvia moderada", ignoreCase = true) -> "⛈️"
        descripcion.contains("nubes", ignoreCase = true) -> "☁️"
        descripcion.contains("cielo claro", ignoreCase = true) -> "☀️"
        else -> "🌤️"
    }
}
/*val iconoTemperatura = when (temp) {

    in Int.MIN_VALUE..10 -> "❄️"
    in 11..20 -> "🌤️"
    else -> "🔥"
}*/
fun obtenerIconoTemperatura(temp: Int): String {
    return when {
        temp <= 10 -> "🥶"
        temp in 11..20 -> "🙂"
        else -> "🥵"
    }
}
fun obtenerColorYIconoPorTemperatura(temp: Int): Pair<Color, String> {
    return when {
        temp <= 10 -> Pair(Color(0xFFB3E5FC), "🥶") // Azul claro
        temp in 11..20 -> Pair(Color(0xFFC8E6C9), "🙂") // Verde claro
        else -> Pair(Color(0xFFFFCDD2), "🥵") // Rojo claro
    }
}
fun formatearFecha(fechaStr: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = inputFormat.parse(fechaStr) ?: return ""

    val dayFormat = SimpleDateFormat("d", Locale("es", "ES"))
    val monthFormat = SimpleDateFormat("MMMM", Locale("es", "ES"))

    val dia = dayFormat.format(date)
    val mes = monthFormat.format(date).replaceFirstChar { it.uppercase() } // capitalizar mes


    return "$dia de $mes "
}
fun obtenerFechaActual(): String {
    val formato = SimpleDateFormat("EEEE, d MMMM", Locale("es"))
    return formato.format(Date()).replaceFirstChar { it.uppercase() }
}

fun obtenerColoresFondo(clima: String): List<Color> {
    return when {
        clima.contains("lluvia ligera", ignoreCase = true) -> listOf(
            Color(0xFF6D8FB0),
            Color(0xFFB0C4DE)
        )  // Lluvia
        clima.contains("nublado", ignoreCase = true) || clima.contains(
            "nubes",
            ignoreCase = true
        ) -> listOf(Color(0xFFD3D3D3), Color(0xFF808080)) // Nublado
        clima.contains("muy nuboso", ignoreCase = true) || clima.contains(
            "nubes",
            ignoreCase = true
        ) -> listOf(Color(0xFFD3D3D3), Color(0xFF808080)) // Nublado
        else -> listOf(Color(0xFFFFF176), Color(0xFFFFD54F)) // Soleado (por defecto)
    }
}
fun String.capitalizeWords(): String = split(" ").joinToString(" ") {
    it.replaceFirstChar { char -> char.uppercase() }
}
fun filtrarPronosticoMediodia(pronostico: List<PronosticoDia>): List<PronosticoDia> {
    return pronostico.filter { dia ->
        dia.dt_txt.contains("12:00:00")
    }
}
@Composable
fun PantallaClima(viewModel: WeatherViewModel = viewModel()) {
    val clima = viewModel.clima
    val pronostico = viewModel.pronostico
    val error = viewModel.error
    //AjustarBarraDeEstado()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // ✅ Respeta barra de estado y navegación
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        clima?.let {
            Box(modifier = Modifier.weight(0.6f)) {
                MostrarClimaActual(it)
            }
        }

        Box(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SelectorUbicacionButton { lat, lon ->
                viewModel.obtenerClimaPorCoordenadas(lat, lon)
                viewModel.obtenerPronostico(lat, lon)
            }
        }

        pronostico?.let {
            Box(modifier = Modifier.weight(0.3f)) {
                MostrarPronostico(it)
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}


//Muestra la lista obtenida del pronostico
@Composable
fun MostrarPronostico(pronostico: List<PronosticoDia>) {
    //filtra solo el pronostico de las 12:00
    val pronosticoMediodia = filtrarPronosticoMediodia(pronostico)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
    ) {
        item {
            Text(
                "Pronóstico de Mediodía",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(pronosticoMediodia) { dia ->
            val tempPromedio = ((dia.main.temp_min + dia.main.temp_max) / 2).roundToInt()
            //Obtiene el color y el icono por el promedio de la temperatura
            val (colorFondo, iconoTemperatura) = obtenerColorYIconoPorTemperatura(tempPromedio)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = colorFondo)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = formatearFecha(dia.dt_txt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = dia.weather.getOrNull(0)?.description?.capitalizeWords() ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$iconoTemperatura",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Min: ${dia.main.temp_min.roundToInt()}°C",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Max: ${dia.main.temp_max.roundToInt()}°C",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}





//Muestra el clima actual segun las coordenadas
@Composable
fun MostrarClimaActual(clima: ClimaResponse) {
    val temperatura = clima.main.temp.roundToInt()
    val descripcion = clima.weather[0].description?.capitalizeWords() ?: ""
    //Usa icono dependiendo del clima
    val iconoClima = obtenerIconoClima(descripcion)
    //Usa icono dependiendo de la temperatura
    val iconoTemperatura = obtenerIconoTemperatura(temperatura)
    //Usa colores dependiendo del clima
    val coloresFondo = obtenerColoresFondo(descripcion)
    val fechaActual = obtenerFechaActual()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = coloresFondo)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = clima.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                color = Color(0xFF2F324B)
            )

            Text(
                text = fechaActual,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5B5F74)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "$iconoClima",
                fontSize = 144.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "$temperatura°",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2F324B)
                    )
                    Text(
                        text = descripcion,
                        fontSize = 22.sp,
                        color = Color(0xFF2F324B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$iconoTemperatura",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }
    }
}


@Composable
fun SelectorUbicacionButton(onUbicacionSeleccionada: (Double, Double) -> Unit) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val lat = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            Log.d("MapSelector", "Resultado recibido: lat=$lat, lon=$lon")
            onUbicacionSeleccionada(lat, lon)
        }
    }

    Button(
        onClick = {
            val intent = Intent(context, MapSelectorActivity::class.java)
            launcher.launch(intent)
        }
    ) {
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = "Seleccionar ubicación"
        )
        Text("Seleccionar ubicación en mapa")
    }
}


@Composable
fun PantallaSinPermiso(onIntentarVerificarPermiso: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOff,
                contentDescription = "Permiso de ubicación requerido",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Permiso de Ubicación Requerido",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Por favor, conceda el permiso de ubicación para obtener el clima actual de su zona.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onIntentarVerificarPermiso,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Verificar permisos")

            }
        }
    }
}

