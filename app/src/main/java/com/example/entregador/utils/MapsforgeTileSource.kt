package com.example.entregador.utils

import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

class MapsforgeTileSource(file: File) : XYTileSource(
    "Offline",
    1,  // minZoom
    20, // maxZoom
    256,
    ".png",
    arrayOf(file.absolutePath)
) {
    companion object {
        fun createFromMBTiles(file: File): MapsforgeTileSource {
            return MapsforgeTileSource(file)
        }
    }
}