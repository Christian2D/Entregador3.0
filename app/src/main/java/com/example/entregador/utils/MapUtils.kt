package com.example.entregador.utils

import android.content.Context
import android.util.Log
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import com.example.entregador.services.RouteCalculator
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParserFactory
import kotlin.math.max
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

object MapUtils {

    // Constantes para logging
    private const val TAG = "MapUtils"
    private const val OSM_FILE_NAME = "santos.osm"

    // Classe para armazenar dados de nós OSM
    private data class OsmNode(val id: Long, val lat: Double, val lon: Double)

    // Implementação completa do OSM Handler
    private class OSMHandler : DefaultHandler() {
        val nodes = mutableMapOf<Long, OsmNode>()
        val ways = mutableListOf<List<GeoPoint>>()

        private var currentNode: OsmNode? = null
        private var currentWayNodes = mutableListOf<OsmNode>()

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
            when (qName) {
                "node" -> {
                    val id = attributes?.getValue("id")?.toLongOrNull() ?: return
                    val lat = attributes.getValue("lat")?.toDoubleOrNull() ?: return
                    val lon = attributes.getValue("lon")?.toDoubleOrNull() ?: return
                    currentNode = OsmNode(id, lat, lon)
                }
                "way" -> currentWayNodes.clear()
                "nd" -> {
                    val ref = attributes?.getValue("ref")?.toLongOrNull() ?: return
                    nodes[ref]?.let { currentWayNodes.add(it) }
                }
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            when (qName) {
                "node" -> currentNode?.let { nodes[it.id] = it }
                "way" -> {
                    if (currentWayNodes.isNotEmpty()) {
                        ways.add(currentWayNodes.map { GeoPoint(it.lat, it.lon) })
                    }
                }
            }
        }
    }

    fun loadOSMData(context: Context, callback: (List<List<GeoPoint>>) -> Unit) {
        Thread {
            try {
                val inputStream = context.assets.open(OSM_FILE_NAME)
                val parser = SAXParserFactory.newInstance().newSAXParser()
                val handler = OSMHandler()

                parser.parse(inputStream, handler)
                inputStream.close()

                val validWays = handler.ways.filter { way ->
                    way.all { GeoUtils.isInBounds(it) }
                }

                // Retorna para a UI Thread
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(validWays)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing OSM data: ${e.message}")
            }
        }.start()
    }

    suspend fun loadOSMDataBlocking(context: Context): List<List<GeoPoint>> {
        return withContext(Dispatchers.IO) {
            // Aqui você coloca o conteúdo da antiga função loadOSMData
            // Mas em vez de usar callback, apenas retorna direto os dados processados
            val calculator = RouteCalculator(context)
            val ways = calculator.loadMapData()  // ou o que você estiver usando
            ways
        }
    }

    fun drawRoute(mapView: MapView, points: List<GeoPoint>, color: Int, width: Float) {
        mapView.post {
            mapView.overlays.removeIf { it is Polyline }
            Polyline().apply {
                setPoints(points)
                outlinePaint.color = color
                outlinePaint.strokeWidth = width
                mapView.overlays.add(this)
            }
            mapView.invalidate()
        }
    }

    fun calculateDeliveryValue(distance: Double): Double {
        return max(7.0, 2.0 + (distance * 2.0))
    }

    fun formatRouteDetails(origin: String, destination: String, distance: Double, value: Double): String {
        return """
            *Detalhes da Entrega*
            Origem: $origin
            Destino: $destination
            Distância: ${"%.2f".format(distance)} km
            Valor: R$ ${"%.2f".format(value)}
        """.trimIndent()
    }
}