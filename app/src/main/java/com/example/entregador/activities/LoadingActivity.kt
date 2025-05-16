package com.example.entregador.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.entregador.R
import com.example.entregador.databinding.ActivityLoadingBinding
import com.example.entregador.services.GraphHopperManager
import com.example.entregador.utils.PermissionUtils
import com.example.entregador.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding
    private lateinit var textLoading: TextView
    private lateinit var graphHopperManager: GraphHopperManager
    private var pontoCount = 0
    private val handler = Handler(Looper.getMainLooper())

    private val arquivoZipCache = "graph-cache.zip"
    private val pastaGraphCache = "graph-cache"
    private val arquivoEnderecos = "enderecos.csv"
    private val arquivoMapa = "santos_map.mbtiles"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        textLoading = binding.textLoading

        if (!PermissionUtils.temPermissaoLocalizacao(this)) {
            PermissionUtils.solicitarPermissaoLocalizacao(this)
        }

        iniciarAnimacaoTexto()
        iniciarCopiaCompleta()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciarAnimacaoTexto() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                pontoCount = (pontoCount % 3) + 1
                textLoading.text = getString(R.string.loading_dots, ".".repeat(pontoCount))
                handler.postDelayed(this, 500)
            }
        }, 500)
    }

    private fun iniciarCopiaCompleta() {
        lifecycleScope.launch {
            var sucesso = true

            withContext(Dispatchers.IO) {
                try {
                    // Copiar grafo zipado
                    val zipDestino = File(filesDir, arquivoZipCache)
                    if (!zipDestino.exists()) {
                        atualizarStatus("Copiando $arquivoZipCache...", 10)
                        assets.open(arquivoZipCache).use { input ->
                            zipDestino.outputStream().use { output -> input.copyTo(output) }
                        }
                        Log.d("Loading", "Copiado: $arquivoZipCache")
                    }

                    // Descompactar grafo
                    val pastaGrafo = File(filesDir, pastaGraphCache)
                    if (!pastaGrafo.exists()) {
                        atualizarStatus("Descompactando grafo...", 30)
                        ZipUtils.unzipFromAssets(zipDestino.inputStream(), pastaGrafo)
                        Log.d("Loading", "Descompactado para: ${pastaGrafo.absolutePath}")
                    }

                    // Copiar endereços
                    val enderecosDestino = File(filesDir, arquivoEnderecos)
                    if (!enderecosDestino.exists()) {
                        atualizarStatus("Copiando endereços...", 50)
                        assets.open(arquivoEnderecos).use { input ->
                            enderecosDestino.outputStream().use { output -> input.copyTo(output) }
                        }
                        Log.d("Loading", "Copiado: $arquivoEnderecos")
                    }

                    // Copiar o mapa
                    val mapaDestino = File(filesDir, arquivoMapa)
                    if (!mapaDestino.exists()) {
                        atualizarStatus("Copiando mapa visual...", 60)
                        assets.open(arquivoMapa).use { input ->
                            mapaDestino.outputStream().use { output -> input.copyTo(output) }
                        }
                        Log.d("Loading", "Copiado: $arquivoMapa")
                    }

                } catch (e: Exception) {
                    Log.e("Loading", "Erro durante cópia ou descompactação", e)
                    sucesso = false
                }
            }

            if (sucesso) {
                atualizarStatus("Carregando grafo de rotas...", 75)

                graphHopperManager = GraphHopperManager(this@LoadingActivity)

                val grafoOk = carregarGraphhopper()

                if (grafoOk) {
                    atualizarStatus("Preparando o app...", 95)
                    delay(500)
                    marcarCopiaFinalizada()
                    iniciarMainActivity()
                } else {
                    Toast.makeText(this@LoadingActivity, "Erro ao carregar o grafo de rotas", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this@LoadingActivity, "Erro durante a preparação do app", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun carregarGraphhopper(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("LoadingActivity", "Carregando grafo com GraphHopperManager...")
            graphHopperManager.loadGraphAsync().also {
                Log.d("LoadingActivity", "GraphHopper carregado? $it")
            }
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Erro ao carregar GraphHopper", e)
            false
        }
    }

    private suspend fun atualizarStatus(msg: String, progresso: Int) {
        withContext(Dispatchers.Main) {
            binding.textLoading.text = msg
            binding.circularProgressView.setProgress(progresso)
        }
    }

    private fun iniciarMainActivity() {
        Log.d("LoadingActivity", "Tudo pronto. Iniciando MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("grafoCarregado", true)
        startActivity(intent)
        finish()
    }

    private fun marcarCopiaFinalizada() {
        getSharedPreferences("config", MODE_PRIVATE).edit {
            putBoolean("copias_finalizadas", true)
        }
    }
}