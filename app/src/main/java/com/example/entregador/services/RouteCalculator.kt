@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.example.entregador.services

import android.content.Context
import com.example.entregador.utils.GeoUtils
import org.osmdroid.util.GeoPoint
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.*
import javax.xml.parsers.SAXParserFactory
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RouteCalculator(private val context: Context) {
    private val graph = HashMap<GeoPoint, MutableList<Pair<GeoPoint, Double>>>()

    // NÃO inicializa aqui!
    // init {
    //     buildGraphFromOSM(context)
    // }

    suspend fun initializeGraph() = withContext(Dispatchers.IO) {
        buildGraphFromOSM(context)
    }

    private fun buildGraphFromOSM(context: Context) {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val handler = object : DefaultHandler() {
            private val nodes = mutableMapOf<Long, GeoPoint>()
            private var currentWay: MutableList<GeoPoint>? = null

            override fun startElement(uri: String?, localName: String?, qName: String?, attrs: Attributes?) {
                when (qName?.lowercase()) {
                    "node" -> handleNode(attrs)
                    "way" -> currentWay = mutableListOf()
                    "nd" -> handleWayNode(attrs)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                if (qName.equals("way", true)) {
                    currentWay?.let { way ->
                        way.zipWithNext().forEach { (a, b) ->
                            val distance = a.distanceTo(b) / 1000 // Convert to kilometers
                            graph.getOrPut(a) { mutableListOf() }.add(b to distance)
                            graph.getOrPut(b) { mutableListOf() }.add(a to distance)
                        }
                    }
                    currentWay = null
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

            private fun handleWayNode(attrs: Attributes?) {
                val ref = attrs?.getValue("ref")?.toLongOrNull() ?: return
                nodes[ref]?.let { node ->
                    currentWay?.add(node)
                }
            }
        }

        context.assets.open("santos.osm").use {
            parser.parse(it, handler)
        }
    }
    fun loadMapData(): List<List<GeoPoint>> {
        return graph.map { entry ->
            listOf(entry.key) + entry.value.map { it.first }
        }
    }

    fun findShortestPath(start: GeoPoint, end: GeoPoint): Pair<List<GeoPoint>, Double>? {
        val distances = mutableMapOf<GeoPoint, Double>().withDefault { Double.MAX_VALUE }
        val predecessors = mutableMapOf<GeoPoint, GeoPoint?>()
        val queue = PriorityQueue<GeoPoint>(compareBy { distances.getValue(it) })

        distances[start] = 0.0
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current == end) break

            graph[current]?.forEach { (neighbor, edgeDistance) ->
                val newDist = distances.getValue(current) + edgeDistance
                if (newDist < distances.getValue(neighbor)) {
                    distances[neighbor] = newDist
                    predecessors[neighbor] = current
                    queue.add(neighbor)
                }
            }
        }

        val path = reconstructPath(end, predecessors)
        val distance = distances[end] ?: Double.MAX_VALUE

        return if (path.isNotEmpty() && distance < Double.MAX_VALUE) {
            path to distance
        } else {
            null // Rota não encontrada
        }
    }

    private fun reconstructPath(end: GeoPoint, predecessors: Map<GeoPoint, GeoPoint?>): List<GeoPoint> {
        val path = mutableListOf<GeoPoint>()
        var current: GeoPoint? = end

        while (current != null) {
            path.add(current)
            current = predecessors[current]
        }

        return path.asReversed().takeIf { it.first() == end } ?: emptyList()
    }

    private fun GeoPoint.distanceTo(other: GeoPoint): Double {
        val dx = latitude - other.latitude
        val dy = longitude - other.longitude
        return sqrt(dx * dx + dy * dy) * 111319.5 // Convert degrees to meters
    }

    private fun GeoPoint.isWithinBounds() = GeoUtils.isInBounds(this)
}