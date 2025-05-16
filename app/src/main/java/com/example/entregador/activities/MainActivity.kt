package com.example.entregador.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.entregador.R
import com.example.entregador.databinding.ActivityMainBinding
import com.example.entregador.services.GraphHopperManager
import com.example.entregador.services.OfflineGeocoder
import com.example.entregador.utils.EnderecoFinder
import com.example.entregador.utils.MapUtils
import com.example.entregador.utils.PermissionUtils
import com.example.entregador.utils.TileSourceFactory
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var enderecoFinder: EnderecoFinder
    private lateinit var offlineGeocoder: OfflineGeocoder
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private lateinit var graphHopperManager: GraphHopperManager
    private var localizacaoAtual: Location? = null
    private var indoParaDestino = true

    // Referência aos marcadores para poder remover depois
    private var markerOrigem: Marker? = null
    private var markerDestino: Marker? = null

    // Constantes
    companion object {
        private const val TAXA_DESLOCAMENTO_KM = 0.30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializações
        Configuration.getInstance().load(applicationContext, getSharedPreferences("prefs", MODE_PRIVATE))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enderecoFinder = EnderecoFinder(this)
        offlineGeocoder = OfflineGeocoder(this)
        graphHopperManager = GraphHopperManager(this)
        mapView = binding.mapView
        setSupportActionBar(binding.toolbar)

        // Permissão de localização
        if (!PermissionUtils.temPermissaoLocalizacao(this)) {
            PermissionUtils.solicitarPermissaoLocalizacao(this)
        }

        configurarMapa()
        setupUIListeners()
    }

    // Configuração inicial do mapa
    private fun configurarMapa() {
        TileSourceFactory.configurarTileSource(this, mapView)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
    }

    private fun setupUIListeners() {
        // Buscar origem
        binding.btnSearchOrigin.setOnClickListener {
            val endereco = binding.etOrigin.text.toString()
            val ponto = offlineGeocoder.buscarCoordenada(endereco)
            if (ponto != null) {
                removerMarcador("origem")
                markerOrigem = adicionarPino(mapView, ponto, "Origem", R.drawable.ic_pin_origem)
            } else {
                Toast.makeText(this, R.string.error_address_not_found, Toast.LENGTH_SHORT).show()
            }
        }

        // Buscar destino
        binding.btnSearchDestination.setOnClickListener {
            val endereco = binding.etDestination.text.toString()
            val ponto = offlineGeocoder.buscarCoordenada(endereco)
            if (ponto != null) {
                removerMarcador("destino")
                markerDestino = adicionarPino(mapView, ponto, "Destino", R.drawable.ic_pin_destino)
            } else {
                Toast.makeText(this, R.string.error_address_not_found, Toast.LENGTH_SHORT).show()
            }
        }

        // Calcular rota
        binding.btnCalculate.setOnClickListener {
            if (!graphHopperManager.isGraphLoaded()) {
                Toast.makeText(this, R.string.error_route_not_ready, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            handleRouteCalculation()
        }

        // Botão "Cheguei na origem"
        binding.btnChegueiOrigem.setOnClickListener {
            if (markerOrigem == null || markerDestino == null) {
                Toast.makeText(applicationContext, "Defina origem e destino antes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val origem = markerOrigem!!.position
            val destino = markerDestino!!.position

            val (rota, tipo) = if (indoParaDestino) {
                graphHopperManager.calculateRoute(origem, destino) to getString(R.string.destino)
            } else {
                graphHopperManager.calculateRoute(destino, origem) to getString(R.string.origem)
            }

            MapUtils.drawRoute(mapView, rota.points, Color.BLUE, 5f, "rota_volta")
            mapView.controller.animateTo(rota.points.firstOrNull())

            indoParaDestino = !indoParaDestino
            binding.btnChegueiOrigem.text =
                if (indoParaDestino) getString(R.string.abrir_google_maps) else getString(R.string.cheguei_na_origem)

            Toast.makeText(applicationContext, "Rota para $tipo traçada!", Toast.LENGTH_SHORT).show()
        }

        // Botão abrir no Google Maps
        binding.btnAbrirGoogleMaps.setOnClickListener {
            val destino = binding.etDestination.text.toString()
            val ponto = offlineGeocoder.buscarCoordenada(destino)
            if (ponto != null) {
                val uri =
                    "geo:${ponto.latitude},${ponto.longitude}?q=${ponto.latitude},${ponto.longitude}(Destino)".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.error_address_not_found, Toast.LENGTH_SHORT).show()
            }
        }

        // Botão WhatsApp
        binding.btnWhatsApp.setOnClickListener {
            val texto = "Entrega de ${binding.etOrigin.text} até ${binding.etDestination.text}\n\n" +
                    "Distância: ${binding.tvDistance.text}\n" +
                    "Tempo estimado: ${binding.tvTime.text}\n" +
                    "Total: ${binding.tvTotal.text}"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data =
                    "https://api.whatsapp.com/send?phone=5513991337370&text=${Uri.encode(texto)}".toUri()
            }
            startActivity(intent)
        }
    }

    private fun handleRouteCalculation() {
        val ori = binding.etOrigin.text.toString()
        val dest = binding.etDestination.text.toString()
        if (ori.isBlank() || dest.isBlank()) {
            Toast.makeText(this, R.string.error_invalid_address, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val origemCoord = enderecoFinder.buscarCoordenada(ori)
            val destinoCoord = enderecoFinder.buscarCoordenada(dest)
            val locAtual = localizacaoAtual

            if (origemCoord == null || destinoCoord == null || locAtual == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, R.string.error_invalid_values, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val geoAtual = GeoPoint(locAtual.latitude, locAtual.longitude)
            val origemLoc = Location("origem").apply {
                latitude = origemCoord.latitude
                longitude = origemCoord.longitude
            }

            val (rotaAteOrigem, rotaEntrega) = graphHopperManager.calculateTwoRoutes(
                current = geoAtual,
                origin = origemCoord,
                destination = destinoCoord
            )

            val deslocKm = graphHopperManager.calculateDisplacementDistance(locAtual, origemLoc)
            val entregaKm = rotaEntrega.distanceKm
            val deslocCusto = deslocKm * TAXA_DESLOCAMENTO_KM

            val valorEntrega = MapUtils.calculateDeliveryValue(entregaKm)
            val valorTotal = valorEntrega + deslocCusto
            val tempoEstimado = (entregaKm * 3).toInt()

            withContext(Dispatchers.Main) {
                mapView.overlays.removeAll { it is org.osmdroid.views.overlay.Polyline }

                MapUtils.drawRoute(mapView, rotaAteOrigem.points, ContextCompat.getColor(this@MainActivity, R.color.red), 6f, "rota_deslocamento")
                MapUtils.drawRoute(mapView, rotaEntrega.points, ContextCompat.getColor(this@MainActivity, R.color.greenPrimary), 6f, "rota_entrega")

                binding.tvDistance.text = getString(R.string.distancia_formatada, entregaKm)
                binding.tvTime.text = getString(R.string.tempo_estimado_formatado, tempoEstimado)
                binding.tvTotal.text = getString(R.string.valor_total_formatado, valorTotal)
            }
        }
    }

    private fun removerMarcador(tipo: String) {
        mapView.overlays.removeAll { it is Marker && it.title == tipo }
    }

    private fun adicionarPino(map: MapView, ponto: GeoPoint, titulo: String, iconeId: Int): Marker {
        return Marker(map).apply {
            position = ponto
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = titulo
            icon = ContextCompat.getDrawable(this@MainActivity, iconeId)
            map.overlays.add(this)
            map.controller.animateTo(ponto)
            map.invalidate()
        }
    }

    // Permissões
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.error_gps_disabled, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_manual_mode -> {
                startActivity(Intent(this, ManualCalcActivity::class.java))
                true
            }
            R.id.menu_about -> {
                Toast.makeText(this, "Desenvolvido por Christian", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}