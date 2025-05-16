package com.example.entregador.utils

import android.content.Context
import android.util.Log
import org.osmdroid.util.GeoPoint
import java.io.*
import java.text.Normalizer
import kotlin.math.abs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

class EnderecoFinder(private val context: Context) {

    data class Endereco(val rua: String, val numero: Int, val lat: Double, val lon: Double)

    private var enderecosCarregados: List<Endereco>? = null
    private val gson = Gson()

    private fun carregarEnderecos(): List<Endereco> {
        if (enderecosCarregados != null) return enderecosCarregados!!

        val cacheFile = File(context.cacheDir, "enderecos_cache.json")

        // Tenta carregar do cache
        try {
            if (cacheFile.exists()) {
                cacheFile.bufferedReader().use {
                    val type = object : TypeToken<List<Endereco>>() {}.type
                    enderecosCarregados = gson.fromJson(it, type)
                    Log.d("EnderecoFinder", "Endereços carregados do cache.")
                    return enderecosCarregados!!
                }
            }
        } catch (e: Exception) {
            Log.e("EnderecoFinder", "Erro ao carregar cache de endereços", e)
        }

        // Cache não encontrado, carrega do CSV
        val lista = mutableListOf<Endereco>()
        try {
            context.assets.open("enderecos.csv").bufferedReader().useLines { linhas ->
                linhas.forEachIndexed { index, linha ->
                    val partes = linha.split(",")
                    if (partes.size >= 7) {
                        val rua = partes[0].trim().normalize()
                        val numero = partes[1].trim().toIntOrNull()
                        val lat = partes[5].trim().toDoubleOrNull()
                        val lon = partes[6].trim().toDoubleOrNull()

                        if (numero != null && lat != null && lon != null) {
                            val ponto = GeoPoint(lat, lon)
                            if (GeoUtils.isInBounds(ponto)) {
                                lista.add(Endereco(rua, numero, lat, lon))
                            }
                        }
                    } else {
                        Log.w("EnderecoFinder", "Linha inválida no CSV (linha $index): $linha")
                    }
                }
            }

            enderecosCarregados = lista

            // Salva o cache com corrotina (luxo total)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    cacheFile.bufferedWriter().use { writer ->
                        gson.toJson(lista, writer)
                    }
                    Log.d("EnderecoFinder", "Cache salvo com sucesso.")
                } catch (e: Exception) {
                    Log.e("EnderecoFinder", "Erro ao salvar cache", e)
                }
            }

        } catch (e: Exception) {
            Log.e("EnderecoFinder", "Erro ao carregar endereços do CSV", e)
        }

        return enderecosCarregados ?: emptyList()
    }

    fun buscarCoordenada(enderecoCompleto: String): GeoPoint? {
        if (enderecoCompleto.isBlank()) return null

        val enderecoLimpo = enderecoCompleto.normalize()
        val regex = Regex("""(.+?)\s*[,\\s]?\s*(\d+)""")
        val match = regex.find(enderecoLimpo)

        if (match != null) {
            val ruaBusca = match.groupValues[1].normalize()
            val numeroBusca = match.groupValues[2].toIntOrNull() ?: return null

            val enderecos = carregarEnderecos()

            // Versão PRO LUXO da busca mais próxima
            return enderecos
                .filter { it.rua.contains(ruaBusca) }
                .minByOrNull { abs(numeroBusca - it.numero) }
                ?.let { GeoPoint(it.lat, it.lon) }
        }

        return null
    }

    private fun String.normalize(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .uppercase()
            .replace(Regex("[^A-Z0-9 ,]"), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}