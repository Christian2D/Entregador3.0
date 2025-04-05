package com.example.entregador.utils

import org.osmdroid.util.GeoPoint

object GeoUtils {
    const val MIN_LAT = -24.04
    const val MAX_LAT = -23.842
    const val MIN_LON = -46.514
    const val MAX_LON = -46.181

    fun isInBounds(point: GeoPoint): Boolean {
        return point.latitude >= MIN_LAT && point.latitude <= MAX_LAT &&
                point.longitude >= MIN_LON && point.longitude <= MAX_LON
    }
}