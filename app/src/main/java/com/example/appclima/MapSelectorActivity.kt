package com.example.appclima


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapSelectorActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var marcador: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selector)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        Log.d("MapSelector", "Mapa listo")

        // habilitar controles
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true

        // Ni bien abre muestra lima
        val lima = LatLng(-12.0464, -77.0428)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12f))

        // listener clic
        googleMap.setOnMapClickListener { latLng ->
            Log.d("MapSelector", "Mapa clic en: $latLng")
            marcador?.remove()
            marcador = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Nueva ubicación")
            )
            mostrarDialogoConfirmacion(latLng)
        }
    }

    //Muestra dialogo de confirmacion de cambio de posición
    private fun mostrarDialogoConfirmacion(latLng: LatLng) {
        if (!isFinishing && !isDestroyed) {
            AlertDialog.Builder(
                this@MapSelectorActivity,
                androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert
            )

                .setTitle("Cambiar ubicación")
                .setMessage("¿Quieres cambiar tu ubicación?")
                .setPositiveButton("Sí") { _, _ ->
                    val resultIntent = Intent().apply {
                        putExtra("latitude", latLng.latitude)
                        putExtra("longitude", latLng.longitude)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .show()
        }

    }


}
