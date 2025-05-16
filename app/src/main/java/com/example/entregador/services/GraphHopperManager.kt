package com.example.entregador.services

import android.content.Context
import android.location.Location
import android.util.Log
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.ResponsePath
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint
import com.example.entregador.utils.ZipUtils
import java.io.File
import kotlin.system.measureTimeMillis

class GraphHopperManager(private val context: Context) {
    private var hopper: GraphHopper? = null

    companion object {
        private const val TAG = "GraphHopperManager"
        private const val PROFILE_NAME = "bike"
        private const val GRAPH_FOLDER = "graph-cache"
    }

    data class RouteResult(val points: List<GeoPoint>, val distanceKm: Double)

    // Carrega o grafo previamente gerado com o GraphHopper 11
    suspend fun loadGraphAsync(onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            val graphDir = File(context.filesDir, GRAPH_FOLDER)

            if (!graphDir.exists()) {
                Log.i(TAG, "Extraindo graph-cache.zip do assets para ${graphDir.absolutePath}")
                try {
                    context.assets.open("graph-cache.zip").use { inputStream ->
                        ZipUtils.unzipFromAssets(inputStream, graphDir)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao extrair o grafo: ${e.message}", e)
                    return@withContext false
                }
            }

            val tempHopper = GraphHopper().apply {
                graphHopperLocation = graphDir.absolutePath
                isAllowWrites = false // Garantir modo somente leitura
            }

            val progressJob = CoroutineScope(Dispatchers.IO).launch {
                var progress = 85
                while (progress < 99) {
                    onProgress(progress++)
                    delay(300)
                }
            }

            val tempo = measureTimeMillis {
                tempHopper.load()
            }

            progressJob.cancelAndJoin()
            onProgress(100)

            hopper = tempHopper
            Log.i(TAG, "Grafo carregado com sucesso em ${tempo}ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar grafo: ${e.message}", e)
            false
        }
    }

    fun isGraphLoaded(): Boolean = hopper != null

    fun calculateRoute(from: GeoPoint, to: GeoPoint): RouteResult {
        val h = hopper ?: throw IllegalStateException("Grafo não carregado!")
        Log.d(TAG, "Calculando rota de ${from.latitude},${from.longitude} até ${to.latitude},${to.longitude}")

        val req = GHRequest(from.latitude, from.longitude, to.latitude, to.longitude)
            .setProfile(PROFILE_NAME)

        val path: ResponsePath = h.route(req).best

        if (path.hasErrors()) throw RuntimeException("Erro na rota: ${path.errors}")

        val pts = path.points.map { GeoPoint(it.lat, it.lon) }
        val distKm = path.distance / 1000.0

        Log.i(TAG, "Rota calculada: ${distKm}km, pontos: ${pts.size}")
        return RouteResult(pts, distKm)
    }

    fun calculateDisplacementDistance(delivererLocation: Location, originLocation: Location): Double {
        return delivererLocation.distanceTo(originLocation) / 1000.0
    }

    fun calculateTwoRoutes(
        current: GeoPoint,
        origin: GeoPoint,
        destination: GeoPoint
    ): Pair<RouteResult, RouteResult> {
        if (!isGraphLoaded()) throw IllegalStateException("Grafo não carregado!")
        val deslocamento = calculateRoute(current, origin)
        val entrega = calculateRoute(origin, destination)
        return deslocamento to entrega
    }
}