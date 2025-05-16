package com.example.entregador.utils

import android.util.Log
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max

object MapUtils {

    /**
     * Desenha uma rota no mapa, removendo qualquer rota anterior com o mesmo ID.
     * Para melhorar performance, reduz o número de pontos se exceder um limite.
     */
    fun drawRoute(
        mapView: MapView?,
        points: List<GeoPoint>?,
        color: Int,
        width: Float,
        id: String = "rota_padrao"
    ) {
        if (mapView == null || points.isNullOrEmpty()) {
            Log.e("MapUtils", "Falha ao desenhar rota: mapView=${mapView != null}, pontos=${points?.size ?: 0}")
            return
        }

        val maxPoints = 200
        val filteredPoints = if (points.size > maxPoints) {
            val step = (points.size / maxPoints).coerceAtLeast(1)
            points.filterIndexed { index, _ -> index % step == 0 }
        } else {
            points
        }

        val polyline = Polyline().apply {
            this.id = id
            setPoints(filteredPoints)
            outlinePaint.color = color
            outlinePaint.strokeWidth = width
        }

        mapView.post {
            mapView.overlays.removeAll { it is Polyline && it.id == id }
            mapView.overlays.add(polyline)
            mapView.invalidate()
        }
    }

    /**
     * Calcula o valor da entrega baseado na distância.
     * - Valor mínimo: R$7,00
     * - Fórmula: R$2,00 + R$2,00 por km
     */
    fun calculateDeliveryValue(distance: Double): Double {
        return max(7.0, 2.0 + distance * 2.0)
    }
}