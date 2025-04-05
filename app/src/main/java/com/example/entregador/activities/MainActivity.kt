package com.example.entregador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.util.Log // IMPORTANTE: no topo do arquivo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.entregador.R
import com.example.entregador.databinding.ActivityMainBinding
import com.example.entregador.services.OfflineGeocoder
import com.example.entregador.services.RouteCalculator
import com.example.entregador.utils.MapUtils
import com.example.entregador.utils.MapsforgeTileSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var geocoder: OfflineGeocoder
    private lateinit var routeCalculator: RouteCalculator
    private var startPoint: GeoPoint? = null
    private var endPoint: GeoPoint? = null

    // Marcadores para origem e destino
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // CORROTINA PARA CARREGAR MAPA SEM TRAVAR
        lifecycleScope.launch(Dispatchers.IO) {
            initializeServicesSafely()
            //val ways = MapUtils.loadOSMDataBlocking(this@MainActivity)
            //withContext(Dispatchers.Main) {
                //MapUtils.drawRoute(binding.mapView, ways.flatten(), ContextCompat.getColor(this@MainActivity, R.color.black), 2f)
            //}
        }

        configureOSMMap()
        initializeServices()
        setupUIListeners()
    }

    private suspend fun initializeServicesSafely() {
        withContext(Dispatchers.IO) {
            geocoder = OfflineGeocoder(this@MainActivity)
            routeCalculator = RouteCalculator(this@MainActivity)
            routeCalculator.initializeGraph()
        }
    }

    private fun configureOSMMap() {
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(getExternalFilesDir(null), "osmdroid")
        }

        loadOfflineMap()

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(-23.9608, -46.3336))
            setMultiTouchControls(true)
        }
    }

    private fun loadOfflineMap() {
        try {
            val mbtilesFile = File(getExternalFilesDir(null), "santos.osm")
            if (mbtilesFile.exists()) {
                binding.mapView.setTileSource(MapsforgeTileSource.createFromMBTiles(mbtilesFile))
            }
        } catch (_: Exception) {
            showToast(R.string.error_offline_map)
        }
    }

    private fun initializeServices() {
        geocoder = OfflineGeocoder(this)
        routeCalculator = RouteCalculator(this)
    }

    private fun setupUIListeners() {
        binding.btnSearchOrigin.setOnClickListener { handleAddressSearch(true) }
        binding.btnSearchDestination.setOnClickListener { handleAddressSearch(false) }
        binding.btnCalculate.setOnClickListener { handleRouteCalculation() }
        binding.btnWhatsApp.setOnClickListener { shareRouteViaWhatsApp() }
    }

    private fun handleAddressSearch(isOrigin: Boolean) {
        val query = if (isOrigin) binding.etOrigin.text.toString()
        else binding.etDestination.text.toString()

        if (query.isBlank()) {
            showToast(R.string.error_invalid_address)
            return
        }

        val results = geocoder.searchAddress(query)
        if (results.isEmpty()) {
            showToast(R.string.error_address_not_found)
            return
        }

        val snappedPoint = geocoder.snapToRoad(results.first()) ?: run {
            showToast(R.string.error_snap_failed)
            return
        }

        val title = if (isOrigin) R.string.label_origin else R.string.label_destination
        addMapMarker(
            snappedPoint,
            getString(title),
            if (isOrigin) R.drawable.ic_pin_start else R.drawable.ic_pin_end,
            isOrigin
        )

        if (isOrigin) startPoint = snappedPoint else endPoint = snappedPoint
        binding.mapView.controller.animateTo(snappedPoint)
    }

    private fun addMapMarker(point: GeoPoint, title: String, iconRes: Int, isOrigin: Boolean) {
        // Remove apenas o marcador correspondente (origem ou destino)
        if (isOrigin) {
            originMarker?.let { binding.mapView.overlays.remove(it) }
        } else {
            destinationMarker?.let { binding.mapView.overlays.remove(it) }
        }

        // Cria e configura o novo marcador
        val marker = Marker(binding.mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            icon = ContextCompat.getDrawable(this@MainActivity, iconRes)
        }

        // Adiciona o marcador ao mapa
        binding.mapView.overlays.add(marker)

        // Armazena a referência do marcador
        if (isOrigin) {
            originMarker = marker
        } else {
            destinationMarker = marker
        }

        binding.mapView.invalidate()
    }

    private fun handleRouteCalculation() {
        val start = startPoint ?: return showToast(R.string.validation_origin_required)
        val end = endPoint ?: return showToast(R.string.validation_destination_required)

        Log.d("ROTA_DEBUG", "StartPoint: ${start.latitude}, ${start.longitude}")
        Log.d("ROTA_DEBUG", "EndPoint: ${end.latitude}, ${end.longitude}")

        val result = routeCalculator.findShortestPath(start, end)

        if (result == null) {
            Log.e("ROUTE", "Rota não encontrada (null)")
            showToast(R.string.error_route_not_found)
            return
        }

        val (path, distance) = result

        if (path.isEmpty() || distance.isInfinite() || distance.isNaN() || distance <= 0.0 || distance > 100.0) {
            Log.e("ROUTE", "Rota inválida ou distância fora do esperado: $distance")
            showToast(R.string.error_route_not_found)
            return
        }

        MapUtils.drawRoute(
            binding.mapView,
            path,
            ContextCompat.getColor(this, R.color.greenPrimary),
            12f
        )

        updateRouteInfo(distance)
    }

    private fun updateRouteInfo(distance: Double) {
        val value = MapUtils.calculateDeliveryValue(distance)
        with(binding) {
            tvValue.text = getString(R.string.template_value, value)
            tvDistance.text = getString(R.string.template_distance, distance)
            tvTime.text = getString(R.string.template_time, distance * 3)
        }
    }

    private fun shareRouteViaWhatsApp() {
        val message = MapUtils.formatRouteDetails(
            origin = binding.etOrigin.text.toString(),
            destination = binding.etDestination.text.toString(),
            distance = binding.tvDistance.text.toString().replace(" km", "").toDoubleOrNull() ?: 0.0,
            value = binding.tvValue.text.toString().replace("R$", "").toDoubleOrNull() ?: 0.0
        )

        try {
            Intent(Intent.ACTION_VIEW).apply {
                // Substitua Uri.parse() pela extensão KTX:
                data = "https://api.whatsapp.com/send".toUri().buildUpon()
                    .appendQueryParameter("phone", "5513991337370")
                    .appendQueryParameter("text", message)
                    .build()
                startActivity(this)
            }
        } catch (_: Exception) {
            showToast(R.string.error_whatsapp)
        }
    }

    private fun showToast(@StringRes messageRes: Int) {
        Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}