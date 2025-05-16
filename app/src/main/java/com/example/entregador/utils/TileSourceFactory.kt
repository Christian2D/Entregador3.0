package com.example.entregador.utils

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory as OSMDroidTileSourceFactory
import org.osmdroid.views.MapView

object TileSourceFactory {
    fun configurarTileSource(context: Context, mapView: MapView) {
        // Carrega configurações do OSM
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))

        // Fonte de tiles personalizada com zoom máximo alto para nitidez
        val tileSource = XYTileSource(
            "MapBox",  // Nome interno
            1,         // Zoom mínimo
            20,        // Zoom máximo (mais detalhado)
            256,       // Tamanho do tile
            ".png",    // Extensão dos tiles
            arrayOf(   // URLs dos tiles do OpenStreetMap
                "https://a.tile.openstreetmap.org/",
                "https://b.tile.openstreetmap.org/",
                "https://c.tile.openstreetmap.org/"
            )
        )

        mapView.setTileSource(tileSource)
    }
}