package com.example.entregador.services

import android.content.Context
import com.example.entregador.utils.GeoUtils
import org.osmdroid.util.GeoPoint
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.Locale
import javax.xml.parsers.SAXParserFactory

class OfflineGeocoder(context: Context) {

    private val streetIndex = HashMap<String, MutableList<GeoPoint>>()
    private val spatialIndex = HashMap<String, MutableList<GeoPoint>>()
    private val gridSize = 0.01 // ~1km grid

    init {
        try {
            android.util.Log.d("OfflineGeocoder", "Tentando carregar e processar santos.osm")
            parseOSMData(context)
            android.util.Log.d("OfflineGeocoder", "Arquivo santos.osm processado com sucesso")
        } catch (e: Exception) {
            android.util.Log.e("OfflineGeocoder", "Erro ao processar santos.osm: ${e.message}", e)
        }
    }

    private fun parseOSMData(context: Context) {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val handler = object : DefaultHandler() {
            private val nodes = HashMap<Long, GeoPoint>()
            private var currentWayPoints = mutableListOf<GeoPoint>()
            private var currentStreet: String? = null

            override fun startElement(uri: String?, localName: String?, qName: String?, attrs: Attributes?) {
                when (qName?.lowercase(Locale.getDefault())) {
                    "node" -> handleNode(attrs)
                    "way" -> resetWay()
                    "tag" -> handleTag(attrs)
                    "nd" -> handleNodeRef(attrs)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                if (qName.equals("way", true)) {
                    currentStreet?.let { street ->
                        if (currentWayPoints.isNotEmpty()) {
                            indexStreet(street, currentWayPoints)
                            indexSpatial(currentWayPoints)
                        }
                    }
                }
            }

            private fun handleNode(attrs: Attributes?) {
                val id = attrs?.getValue("id")?.toLongOrNull() ?: return
                val lat = attrs.getValue("lat")?.toDoubleOrNull() ?: return
                val lon = attrs.getValue("lon")?.toDoubleOrNull() ?: return

                GeoPoint(lat, lon).takeIf { it.isWithinBounds() }?.let {
                    nodes[id] = it
                }
            }

            private fun handleTag(attrs: Attributes?) {
                if (attrs?.getValue("k") == "name") {
                    currentStreet = attrs.getValue("v")
                }
            }

            private fun handleNodeRef(attrs: Attributes?) {
                attrs?.getValue("ref")?.toLongOrNull()?.let { nodeId ->
                    nodes[nodeId]?.let { point ->
                        if (point.isWithinBounds()) {
                            currentWayPoints.add(point)
                        }
                    }
                }
            }

            private fun resetWay() {
                currentWayPoints = mutableListOf()
                currentStreet = null
            }

            private fun indexStreet(street: String, points: List<GeoPoint>) {
                val key = street.lowercase(Locale.getDefault()).trim()
                streetIndex.getOrPut(key) { mutableListOf() }.addAll(points)
            }

            private fun indexSpatial(points: List<GeoPoint>) {
                points.forEach { point ->
                    val gridKey = "${(point.latitude / gridSize).toInt()},${(point.longitude / gridSize).toInt()}"
                    spatialIndex.getOrPut(gridKey) { mutableListOf() }.add(point)
                }
            }
        }

        context.assets.open("santos.osm").use { input ->
            parser.parse(input, handler)
        }
    }

    fun searchAddress(query: String): List<GeoPoint> {
        val cleanQuery = query.lowercase(Locale.getDefault()).trim()
        return streetIndex[cleanQuery]?.distinct() ?: emptyList()
    }

    fun snapToRoad(point: GeoPoint): GeoPoint? {
        val gridKey = "${(point.latitude / gridSize).toInt()},${(point.longitude / gridSize).toInt()}"
        return spatialIndex[gridKey]?.minByOrNull { it.distanceToAsDouble(point) }
    }

    private fun GeoPoint.isWithinBounds() = GeoUtils.isInBounds(this)
}