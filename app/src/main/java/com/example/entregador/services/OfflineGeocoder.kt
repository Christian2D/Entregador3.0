package com.example.entregador.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

class OfflineGeocoder(private val context: Context) {

    private val enderecos: Map<String, GeoPoint> by lazy {
        try {
            context.assets.open("enderecos.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).useLines { lines ->
                    lines.mapNotNull { line ->
                        val parts = line.split(";")
                        if (parts.size == 3) {
                            val nome = parts[0].trim().lowercase(Locale.getDefault())
                            val lat = parts[1].toDoubleOrNull()
                            val lon = parts[2].toDoubleOrNull()
                            if (lat != null && lon != null) nome to GeoPoint(lat, lon) else null
                        } else null
                    }.toMap()
                }
            }
        } catch (e: Exception) {
            Log.e("OfflineGeocoder", "Erro ao carregar endereços do CSV", e)
            emptyMap()
        }
    }

    fun buscarCoordenada(endereco: String): GeoPoint? {
        val chave = endereco.trim().lowercase(Locale.getDefault())
        return enderecos[chave]
    }

    suspend fun sugerirEnderecos(parcial: String): List<String> = withContext(Dispatchers.IO) {
        if (parcial.length < 6) return@withContext emptyList()

        val consulta = parcial.trim().lowercase(Locale.getDefault())
        return@withContext enderecos.keys
            .filter { it.contains(consulta) }
            .take(2) // Limita a 2 resultados
    }
}